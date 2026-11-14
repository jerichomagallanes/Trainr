package com.jericx.trainr.llama

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.util.Log
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed interface CompletionResult {
    data class Text(val text: String) : CompletionResult
    data object Timeout : CompletionResult
    data object TooLong : CompletionResult
    data object Cancelled : CompletionResult
    data object Failed : CompletionResult
    data object OutOfMemory : CompletionResult
}

interface LocalModel {
    suspend fun complete(
        system: String,
        user: String,
        grammar: String,
        maxTokens: Int,
        timeout: Duration
    ): CompletionResult
}

class LlamaEngine(
    private val modelFile: () -> File?,
    private val nativeLibraryDir: String,
    private val availableMemory: () -> Long,
    private val dispatcher: CoroutineDispatcher
) : LocalModel, ComponentCallbacks2 {

    private val mutex = Mutex()

    @Volatile
    private var handle = 0L

    @Volatile
    private var releaseRequested = false

    private val handleLock = Any()

    override suspend fun complete(
        system: String,
        user: String,
        grammar: String,
        maxTokens: Int,
        timeout: Duration
    ): CompletionResult = mutex.withLock {
        val file = modelFile() ?: return CompletionResult.Failed
        releaseRequested = false
        if (handle == 0L) {
            handle = withContext(dispatcher + NonCancellable) {
                try {
                    LlamaNative.loadBackends(nativeLibraryDir)
                    LlamaNative.load(file.absolutePath, threads())
                } catch (error: LinkageError) {
                    Log.e(TAG, "runtime library unavailable", error)
                    0L
                }
            }
            if (handle == 0L) {
                return if (availableMemory() < file.length()) {
                    CompletionResult.OutOfMemory
                } else {
                    CompletionResult.Failed
                }
            }
        }
        val session = handle
        val systemBytes = system.toByteArray()
        val userBytes = user.toByteArray()
        val fit = withContext(dispatcher) {
            LlamaNative.fits(session, systemBytes, userBytes, maxTokens)
        }
        val result = when (fit) {
            LlamaNative.FIT_TOO_LONG -> CompletionResult.TooLong
            LlamaNative.FIT_OK -> coroutineScope {
                val run = async(dispatcher) {
                    LlamaNative.complete(
                        session,
                        systemBytes,
                        userBytes,
                        grammar.toByteArray(),
                        maxTokens
                    )
                }
                try {
                    if (ranWithin(timeout, run)) {
                        run.await()
                            ?.let { CompletionResult.Text(String(it)) }
                            ?: if (releaseRequested) {
                                CompletionResult.Cancelled
                            } else {
                                CompletionResult.Failed
                            }
                    } else {
                        LlamaNative.cancel(session)
                        withContext(NonCancellable) { run.join() }
                        CompletionResult.Timeout
                    }
                } catch (cancelled: CancellationException) {
                    LlamaNative.cancel(session)
                    withContext(NonCancellable) { run.join() }
                    throw cancelled
                }
            }

            else -> CompletionResult.Failed
        }
        if (releaseRequested) release()
        result
    }

    private suspend fun ranWithin(timeout: Duration, run: Deferred<*>): Boolean {
        val budget = ReadBudget(timeout, SLICE)
        while (!budget.isSpent && !run.isCompleted) {
            val mark = TimeSource.Monotonic.markNow()
            withTimeoutOrNull(budget.nextSlice) { run.join() }
            budget.charge(mark.elapsedNow())
        }
        return run.isCompleted
    }

    override fun onTrimMemory(level: Int) {
        if (level < ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) return
        releaseRequested = true
        synchronized(handleLock) { if (handle != 0L) LlamaNative.cancel(handle) }
        if (mutex.tryLock()) {
            try {
                release()
            } finally {
                mutex.unlock()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() = onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    private fun release() {
        val session = synchronized(handleLock) { handle.also { handle = 0L } }
        if (session != 0L) LlamaNative.free(session)
    }

    private fun threads(): Int =
        (Runtime.getRuntime().availableProcessors() - 1).coerceIn(MIN_THREADS, MAX_THREADS)

    private companion object {
        const val TAG = "LlamaEngine"
        const val MIN_THREADS = 2
        const val MAX_THREADS = 6
        val SLICE = 1.seconds
    }
}

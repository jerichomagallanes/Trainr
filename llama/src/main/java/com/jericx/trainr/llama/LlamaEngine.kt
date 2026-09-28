package com.jericx.trainr.llama

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.util.Log
import java.io.File
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

sealed interface CompletionResult {
    data class Text(val text: String) : CompletionResult
    data object Timeout : CompletionResult
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
        val result = coroutineScope {
            val run = async(dispatcher) {
                LlamaNative.complete(
                    session,
                    system.toByteArray(),
                    user.toByteArray(),
                    grammar.toByteArray(),
                    maxTokens
                )
            }
            try {
                withTimeout(timeout) { run.await() }
                    ?.let { CompletionResult.Text(String(it)) }
                    ?: if (releaseRequested) CompletionResult.Cancelled else CompletionResult.Failed
            } catch (timedOut: TimeoutCancellationException) {
                LlamaNative.cancel(session)
                withContext(NonCancellable) { run.join() }
                CompletionResult.Timeout
            } catch (cancelled: CancellationException) {
                LlamaNative.cancel(session)
                withContext(NonCancellable) { run.join() }
                throw cancelled
            }
        }
        if (releaseRequested) release()
        result
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
    }
}

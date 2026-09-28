package com.jericx.trainr.data.model

import com.jericx.trainr.domain.unstuck.intent.InstallFailure
import com.jericx.trainr.domain.unstuck.intent.LocalModelInstaller
import com.jericx.trainr.domain.unstuck.intent.ModelState
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class ModelInstaller(
    private val directory: File,
    private val eligibility: DeviceEligibility,
    private val source: ModelSource,
    private val freeSpace: (File) -> Long,
    dispatcher: CoroutineDispatcher
) : LocalModelInstaller {

    val file = File(directory, ModelArtifact.FILE_NAME)

    private val part = File(directory, "${ModelArtifact.FILE_NAME}.part")

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _state = MutableStateFlow(stateOnDisk())
    override val state: StateFlow<ModelState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        if (_state.value == ModelState.Verifying) job = scope.launch { verify(file) }
    }

    fun readyFile(): File? = file.takeIf { _state.value == ModelState.Ready }

    override fun install() {
        when (_state.value) {
            ModelState.NotInstalled, ModelState.InsufficientStorage, is ModelState.Failed -> Unit
            else -> return
        }
        val partBytes = part.length()
        if (freeSpace(directory) < REQUIRED_FREE_BYTES - partBytes) {
            _state.value = ModelState.InsufficientStorage
            return
        }
        _state.value = ModelState.Downloading(partBytes, ModelArtifact.SIZE_BYTES)
        job = scope.launch { download() }
    }

    override fun cancel() {
        job?.cancel()
    }

    private suspend fun download() {
        try {
            if (part.length() < ModelArtifact.SIZE_BYTES) fetch()
        } catch (cancelled: CancellationException) {
            _state.value = ModelState.NotInstalled
            throw cancelled
        } catch (failure: IOException) {
            _state.value = ModelState.Failed(InstallFailure.DOWNLOAD)
            return
        }
        if (part.length() < ModelArtifact.SIZE_BYTES) {
            _state.value = ModelState.Failed(InstallFailure.DOWNLOAD)
            return
        }
        verify(part)
    }

    private suspend fun fetch() {
        directory.mkdirs()
        source.open(part.length()).use { download ->
            if (download.resumedFrom == 0L) part.delete()
            var done = download.resumedFrom
            FileOutputStream(part, true).use { output ->
                val buffer = ByteArray(BUFFER_BYTES)
                while (true) {
                    val read = download.stream.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    done += read
                    _state.value = ModelState.Downloading(done, ModelArtifact.SIZE_BYTES)
                    yield()
                }
            }
        }
    }

    private suspend fun verify(candidate: File) {
        _state.value = ModelState.Verifying
        val matches = try {
            sha256(candidate) == ModelArtifact.SHA256
        } catch (cancelled: CancellationException) {
            _state.value = ModelState.NotInstalled
            throw cancelled
        } catch (failure: IOException) {
            false
        }
        if (matches && (candidate == file || candidate.renameTo(file))) {
            _state.value = ModelState.Ready
        } else {
            candidate.delete()
            _state.value = ModelState.Failed(InstallFailure.HASH_MISMATCH)
        }
    }

    private suspend fun sha256(candidate: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        candidate.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
                yield()
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun stateOnDisk(): ModelState = when {
        !eligibility.isSupported -> ModelState.Unsupported
        file.length() == ModelArtifact.SIZE_BYTES -> ModelState.Ready
        file.exists() -> ModelState.Verifying
        else -> ModelState.NotInstalled
    }

    private companion object {
        const val BUFFER_BYTES = 256 * 1024
        const val REQUIRED_FREE_BYTES = 2 * ModelArtifact.SIZE_BYTES
    }
}

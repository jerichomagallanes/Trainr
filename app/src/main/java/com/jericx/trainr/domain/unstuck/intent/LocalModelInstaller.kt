package com.jericx.trainr.domain.unstuck.intent

import kotlinx.coroutines.flow.StateFlow

sealed interface ModelState {

    data object Unsupported : ModelState

    data object NotInstalled : ModelState

    data class Downloading(val bytesDone: Long, val bytesTotal: Long) : ModelState

    data object Verifying : ModelState

    data object Ready : ModelState

    data object InsufficientStorage : ModelState

    data class Failed(val kind: InstallFailure) : ModelState
}

enum class InstallFailure { DOWNLOAD, HASH_MISMATCH }

interface LocalModelInstaller {

    val state: StateFlow<ModelState>

    fun install()

    fun cancel()
}

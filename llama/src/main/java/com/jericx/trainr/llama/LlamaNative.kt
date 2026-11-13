package com.jericx.trainr.llama

object LlamaNative {

    // fits() answers three ways, because a prompt that could not be built is a
    // different fact from one that is too long to read.
    const val FIT_UNPREPARED = -1
    const val FIT_TOO_LONG = 0
    const val FIT_OK = 1

    init {
        System.loadLibrary("trainr_llama")
    }

    external fun loadBackends(nativeLibraryDir: String)

    external fun load(path: String, threads: Int): Long

    external fun complete(
        handle: Long,
        system: ByteArray,
        user: ByteArray,
        grammar: ByteArray,
        maxTokens: Int
    ): ByteArray?

    external fun fits(
        handle: Long,
        system: ByteArray,
        user: ByteArray,
        maxTokens: Int
    ): Int

    external fun cancel(handle: Long)

    external fun free(handle: Long)
}

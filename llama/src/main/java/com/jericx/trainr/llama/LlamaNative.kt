package com.jericx.trainr.llama

object LlamaNative {

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
    ): Boolean

    external fun cancel(handle: Long)

    external fun free(handle: Long)
}

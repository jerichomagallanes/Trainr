package com.jericx.trainr.data.model

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelDownload(val stream: InputStream, val resumedFrom: Long) : Closeable {
    override fun close() = stream.close()
}

fun interface ModelSource {
    @Throws(IOException::class)
    fun open(from: Long): ModelDownload
}

class HttpModelSource(private val url: String = ModelArtifact.URL) : ModelSource {

    override fun open(from: Long): ModelDownload {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = READ_TIMEOUT_MILLIS
        if (from > 0) connection.setRequestProperty("Range", "bytes=$from-")
        val resumedFrom = when (val code = connection.responseCode) {
            HttpURLConnection.HTTP_PARTIAL -> from
            HttpURLConnection.HTTP_OK -> 0L
            else -> {
                connection.disconnect()
                throw IOException("HTTP $code")
            }
        }
        return ModelDownload(connection.inputStream, resumedFrom)
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 30_000
    }
}

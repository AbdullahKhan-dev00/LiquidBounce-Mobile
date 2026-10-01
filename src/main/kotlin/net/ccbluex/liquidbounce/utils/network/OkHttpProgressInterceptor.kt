package net.ccbluex.liquidbounce.utils.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.buffer

class OkHttpProgressInterceptor(
    private val progressListener: ProgressListener
) : Interceptor {

    fun interface ProgressListener {
        fun update(bytesRead: Long, contentLength: Long, done: Boolean)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body ?: return response

        return response.newBuilder()
            .body(ProgressResponseBody(body, progressListener))
            .build()
    }

    private class ProgressResponseBody(
        private val responseBody: ResponseBody,
        private val progressListener: ProgressListener
    ) : ResponseBody() {

        override fun contentType() = responseBody.contentType()

        override fun contentLength() = responseBody.contentLength()

        override fun source() = responseBody.source().let { source ->
            object : ForwardingSource(source) {
                private var bytesRead = 0L

                override fun read(sink: Buffer, byteCount: Long): Long {
                    val read = super.read(sink, byteCount)

                    if (read != -1L) {
                        bytesRead += read
                    }

                    progressListener.update(
                        bytesRead,
                        responseBody.contentLength(),
                        read == -1L
                    )

                    return read
                }
            }.buffer()
        }
    }
}

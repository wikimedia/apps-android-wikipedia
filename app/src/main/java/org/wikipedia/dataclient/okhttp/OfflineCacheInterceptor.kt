package org.wikipedia.dataclient.okhttp

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.*
import org.wikipedia.WikipediaApp
import org.wikipedia.offline.OfflineObject
import org.wikipedia.offline.OfflineObjectDbHelper
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import java.io.*
import java.io.IOException
import java.util.*

class OfflineCacheInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response
        val networkException: IOException
        var lang = request.header(LANG_HEADER)
        val title = UriUtil.decodeURL(request.header(TITLE_HEADER).orEmpty())

        // attempt to read from the network.
        try {
            response = chain.proceed(request)
            // is this response worthy of caching offline?
            return if (response.isSuccessful && response.networkResponse != null &&
                shouldSave(request) && !lang.isNullOrEmpty() && title.isNotEmpty()) {
                // Cache (or re-cache) the response, overwriting any previous version.
                getCacheWritingResponse(request, response, lang, title)
            } else response
        } catch (t: IOException) {
            networkException = t
        }

        // If we're here, then the network call failed.
        // Time to see if we can load this content from offline storage.
        val url = request.url.toString()

        // If we don't have the correct headers to retrieve this item, then bail.
        if (lang.isNullOrEmpty()) {
            // ...unless we're looking for an image from Commons, in which case we'll try to match it by URL only.
            lang = if (url.contains("/commons/")) {
                ""
            } else {
                throw networkException
            }
        }
        val obj = OfflineObjectDbHelper.instance().findObject(url, lang)
        if (obj == null) {
            L.w("Offline object not present in database.")
            throw networkException
        }
        val metadataFile = File(obj.path + ".0")
        val contentsFile = File(obj.path + ".1")
        if (!metadataFile.exists() || !contentsFile.exists()) {
            throw IOException("Offline object not present in filesystem.")
        }
        val builder = Response.Builder().request(request).protocol(Protocol.HTTP_2)
        var contentType = "*/*"
        try {
            BufferedReader(InputStreamReader(FileInputStream(metadataFile))).use { reader ->
                reader.readLine() // url
                reader.readLine() // method
                reader.readLine() // protocol
                builder.code(reader.readLine().toInt())
                val message = reader.readLine()
                builder.message(if (message.isNullOrEmpty()) "OK" else message)
                while (true) {
                    val line = reader.readLine() ?: break
                    val pos = line.indexOf(":")
                    if (pos < 0) {
                        break
                    }
                    val name = line.substring(0, pos).trim()
                    val value = line.substring(pos + 1).trim()
                    builder.header(name, value)
                    if (name.lowercase(Locale.getDefault()) == "content-type") {
                        contentType = value
                    }
                }
            }
        } catch (e: IOException) {
            L.e(e)
        }

        // since we're returning this response manually, let's tell the network library not to cache it.
        builder.header("Cache-Control", "no-cache")
        // and tack on the Save header, so that the recipient knows that this response came from offline cache.
        builder.header(SAVE_HEADER, SAVE_HEADER_SAVE)
        builder.body(CachedResponseBody(contentsFile, contentType))
        response = builder.build()
        return response
    }

    private fun getCacheWritingResponse(request: Request, response: Response, lang: String, title: String): Response {
        val contentType = response.header("Content-Type", "*/*")!!
        val contentLength = response.header("Content-Length", "-1")!!.toLong()
        val cachePath = WikipediaApp.getInstance().filesDir.absolutePath + File.separator + OfflineObjectDbHelper.OFFLINE_PATH

        File(cachePath).mkdirs()

        val filePath = cachePath + File.separator + getObjectFileName(request.url.toString(), lang, contentType)
        val metadataFile = File("$filePath.0")
        val contentsFile = File("$filePath.1")
        try {
            OutputStreamWriter(FileOutputStream(metadataFile)).use { writer ->
                writer.write(request.url.toString() + "\n")
                writer.write(request.method + "\n")
                writer.write(response.protocol.toString() + "\n")
                writer.write(response.code.toString() + "\n")
                writer.write(response.message + "\n")
                response.headers.names().forEach { header ->
                    writer.write(header + ": " + response.header(header) + "\n")
                }
                writer.flush()
            }
        } catch (e: IOException) {
            L.e(e)
            return response
        }

        val sink = try {
            contentsFile.sink().buffer()
        } catch (e: IOException) {
            L.e(e)
            return response
        }

        return response.body?.let {
            val obj = OfflineObject(request.url.toString(), lang, filePath, 0)
            val cacheWritingSource = CacheWritingSource(it.source(), sink, obj, title)
            response.newBuilder()
                .body(CacheWritingResponseBody(cacheWritingSource, contentType, contentLength))
                .build()
        } ?: response
    }

    private inner class CacheWritingSource constructor(private val source: BufferedSource, private val cacheSink: BufferedSink,
                                                       private val obj: OfflineObject, private val title: String) : Source {
        private var cacheRequestClosed = false
        private var failed = false

        @Throws(IOException::class)
        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytesRead: Long
            try {
                bytesRead = source.read(sink, byteCount)
            } catch (e: IOException) {
                failed = true
                if (!cacheRequestClosed) {
                    cacheRequestClosed = true
                    // Failed to write a complete cache response.
                }
                throw e
            }
            if (bytesRead == -1L) {
                if (!cacheRequestClosed) {
                    cacheRequestClosed = true
                    // The cache response is complete!
                    cacheSink.close()
                    if (!failed) {
                        // update the record in the database!
                        OfflineObjectDbHelper.instance().addObject(obj.url, obj.lang, obj.path, title)
                    }
                }
                return -1
            }
            sink.copyTo(cacheSink.buffer, sink.size - bytesRead, bytesRead)
            cacheSink.emitCompleteSegments()
            return bytesRead
        }

        override fun timeout(): Timeout {
            failed = true
            return source.timeout()
        }

        @Throws(IOException::class)
        override fun close() {
            if (!cacheRequestClosed) {
                // discard(this, ExchangeCodec.DISCARD_STREAM_TIMEOUT_MILLIS, MILLISECONDS)
                cacheRequestClosed = true
            }
            source.close()
            if (failed) {
                OfflineObjectDbHelper.deleteFilesForObject(obj)
            }
        }
    }

    private inner class CacheWritingResponseBody constructor(private val source: Source,
                                                             private val contentType: String?,
                                                             private val contentLength: Long) : ResponseBody() {
        override fun contentType(): MediaType? {
            return contentType?.toMediaTypeOrNull()
        }

        override fun contentLength(): Long {
            return contentLength
        }

        override fun source(): BufferedSource {
            return source.buffer()
        }
    }

    private inner class CachedResponseBody constructor(private val file: File,
                                                       private val contentType: String?) : ResponseBody() {
        override fun contentType(): MediaType? {
            return contentType?.toMediaTypeOrNull()
        }

        override fun contentLength(): Long {
            return -1
        }

        override fun source(): BufferedSource {
            return file.source().buffer()
        }
    }

    companion object {
        const val LANG_HEADER = "X-Offline-Lang"
        const val TITLE_HEADER = "X-Offline-Title"
        const val SAVE_HEADER = "X-Offline-Save"
        const val SAVE_HEADER_SAVE = "save"

        @JvmStatic
        fun shouldSave(request: Request): Boolean {
            return "GET" == request.method && SAVE_HEADER_SAVE == request.header(SAVE_HEADER)
        }

        private fun getObjectFileName(url: String, lang: String, mimeType: String): String {
            // If the object is an image, then make the hash independent of language.
            // Otherwise, encode the language into the hash.
            return if (mimeType.startsWith("image")) StringUtil.md5string(url) else StringUtil.md5string("$lang:$url")
        }
    }
}

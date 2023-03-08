package org.wikipedia.dataclient.okhttp

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.*
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.offline.db.OfflineObject
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import java.io.*
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeLines

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
        val obj = AppDatabase.instance.offlineObjectDao().findObject(url, lang)
        if (obj == null) {
            L.w("Offline object not present in database.")
            throw networkException
        }
        val metadataPath = Paths.get("${obj.path}.0")
        val contentsPath = Paths.get("${obj.path}.1")
        if (!metadataPath.exists() || !contentsPath.exists()) {
            throw IOException("Offline object not present in filesystem.")
        }
        val builder = Response.Builder().request(request).protocol(Protocol.HTTP_2)
        var contentType = "*/*"
        try {
            val lines = metadataPath.readLines()
            // Items 0-2 are the URL, method and protocol
            builder.code(lines[3].toInt()) // Code
            builder.message(lines[4].ifEmpty { "OK" }) // Message
            for (i in 5 until lines.size) {
                val (name, value) = lines[i].split(":", limit = 2).map { it.trim() }
                builder.header(name, value)
                if (name.equals("content-type", ignoreCase = true)) {
                    contentType = value
                }
            }
        } catch (e: IOException) {
            L.e(e)
        }

        // since we're returning this response manually, let's tell the network library not to cache it.
        builder.header("Cache-Control", "no-cache")
        // and tack on the Save header, so that the recipient knows that this response came from offline cache.
        builder.header(SAVE_HEADER, SAVE_HEADER_SAVE)
        builder.body(CachedResponseBody(contentsPath, contentType))
        response = builder.build()
        return response
    }

    private fun getCacheWritingResponse(request: Request, response: Response, lang: String, title: String): Response {
        val contentType = response.header("Content-Type", "*/*")!!
        val contentLength = response.header("Content-Length", "-1")!!.toLong()
        val cachePath = WikipediaApp.instance.filesDir.toPath().resolve(OFFLINE_PATH)
            .createDirectories()

        val filePath = cachePath.resolve(getObjectFileName(request.url.toString(), lang, contentType))
        val metadataPath = filePath.resolveSibling("${filePath.fileName}.0")
        val contentsPath = filePath.resolveSibling("${filePath.fileName}.1")
        try {
            val lines = listOf(request.url.toString(), request.method, response.protocol.toString(),
                response.code.toString(), response.message)
            val headers = response.headers.map { (name, value) -> "$name: $value" }
            metadataPath.writeLines(lines + headers)
        } catch (e: IOException) {
            L.e(e)
            return response
        }

        val sink = try {
            contentsPath.sink().buffer()
        } catch (e: IOException) {
            L.e(e)
            return response
        }

        return response.body?.let {
            val obj = OfflineObject(url = request.url.toString(), lang = lang,
                path = filePath.toString(), status = 0)
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
                        AppDatabase.instance.offlineObjectDao().addObject(obj.url, obj.lang, obj.path, title)
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
                AppDatabase.instance.offlineObjectDao().deleteFilesForObject(obj)
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

    private inner class CachedResponseBody constructor(private val path: Path,
                                                       private val contentType: String?) : ResponseBody() {
        override fun contentType(): MediaType? {
            return contentType?.toMediaTypeOrNull()
        }

        override fun contentLength(): Long {
            return -1
        }

        override fun source(): BufferedSource {
            return path.source().buffer()
        }
    }

    companion object {
        const val LANG_HEADER = "X-Offline-Lang"
        const val TITLE_HEADER = "X-Offline-Title"
        const val SAVE_HEADER = "X-Offline-Save"
        const val SAVE_HEADER_SAVE = "save"
        const val OFFLINE_PATH = "offline_files"

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

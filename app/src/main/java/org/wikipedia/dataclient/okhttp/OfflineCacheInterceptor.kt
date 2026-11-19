package org.wikipedia.dataclient.okhttp

import androidx.room.withTransaction
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import okio.sink
import okio.source
import okio.use
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.extensions.closeSilently
import org.wikipedia.offline.ArticleCacheRequest
import org.wikipedia.offline.OfflineCacheWrite
import org.wikipedia.offline.db.OfflineObject
import org.wikipedia.util.log.L
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

class OfflineCacheInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val stagingDirectory = responseStagingDirectory
        val networkException: IOException
        var lang = request.header(LANG_HEADER)
        val cacheRequest = request.tag(ArticleCacheRequest::class.java)
        val save = shouldSave(request) && !lang.isNullOrEmpty() && !request.header(TITLE_HEADER).isNullOrEmpty() &&
            (cacheRequest != null || isTalkCacheRequest(request))
        val foregroundWrite = if (save && cacheRequest != null && cacheRequest.download == null) OfflineCacheWrite.start(cacheRequest.key) else null
        val write = cacheRequest?.download ?: foregroundWrite
        var writingBody = false

        // attempt to read from the network.
        try {
            val response = chain.proceed(request)
            // is this response worthy of caching offline?
            if (save && response.isSuccessful && response.networkResponse != null && write?.isCurrent() != false) {
                val cached = getCacheWritingResponse(request, response, lang.orEmpty(), write, foregroundWrite, stagingDirectory)
                writingBody = cached !== response
                return cached
            }
            return response
        } catch (t: IOException) {
            networkException = t
        } finally {
            if (!writingBody) foregroundWrite?.close()
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
        var opened: Response? = null
        try {
            val database = AppDatabase.instance
            return runBlocking {
                database.withTransaction {
                    val obj = database.offlineObjectDao().findObject(url, lang) ?: return@withTransaction null
                    openCachedResponse(request, obj).also { opened = it }
                }
            } ?: throw networkException
        } catch (error: Throwable) {
            opened?.close()
            throw error
        }
    }

    private fun openCachedResponse(request: Request, obj: OfflineObject): Response {
        val metadataFile = File(obj.path + ".0")
        val contentsFile = File(obj.path + ".1")
        val builder = Response.Builder().request(request).protocol(Protocol.HTTP_2)
        val headers = Headers.Builder()
        var contentType = "*/*"
        BufferedReader(InputStreamReader(FileInputStream(metadataFile))).use { reader ->
            reader.readLine() // url
            reader.readLine() // method
            reader.readLine() // protocol
            builder.code(reader.readLine()?.toIntOrNull() ?: throw IOException("Invalid offline response status."))
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
                try {
                    // OkHttp accepts non-ASCII header values from the network, so the saved values
                    // may contain them, but it rejects them when added through the regular methods.
                    headers.addUnsafeNonAscii(name, value)
                } catch (e: IllegalArgumentException) {
                    L.w("Skipping invalid offline header: $name")
                    continue
                }
                if (name.equals("content-type", true)) {
                    contentType = value
                }
            }
        }
        builder.headers(headers.build())

        // since we're returning this response manually, let's tell the network library not to cache it.
        builder.header("Cache-Control", "no-cache")
        // and tack on the Save header, so that the recipient knows that this response came from offline cache.
        builder.header(SAVE_HEADER, SAVE_HEADER_SAVE)
        val body = contentsFile.source().buffer().asResponseBody(contentType.toMediaTypeOrNull())
        return try {
            builder.body(body).build()
        } catch (error: Throwable) {
            body.close()
            throw error
        }
    }

    private fun isTalkCacheRequest(request: Request): Boolean {
        val url = request.url
        return url.encodedPath.endsWith("/api.php") && when (url.queryParameter("action")) {
            "discussiontoolspageinfo" -> true
            "query" -> url.queryParameter("meta") == "siteinfo" && url.queryParameter("siprop") == "namespaces"
            else -> false
        }
    }

    private fun getCacheWritingResponse(request: Request, response: Response, lang: String,
                                        write: OfflineCacheWrite?, foregroundWrite: OfflineCacheWrite?, stagingDirectory: File): Response {
        val contentType = response.header("Content-Type", "*/*")!!
        val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1
        val offlineDirectory = File(WikipediaApp.instance.filesDir, OFFLINE_PATH)
        var staged: OfflineObject? = null
        try {
            stagingDirectory.mkdirs()
            offlineDirectory.mkdirs()
            val metadata = File.createTempFile(RESPONSE_FILE_PREFIX, ".0", stagingDirectory)
            val path = metadata.path.removeSuffix(".0")
            staged = OfflineObject(url = request.url.toString(), lang = lang, path = path, status = 0)
            metadata.bufferedWriter().use { writer ->
                writer.write(request.url.toString() + "\n")
                writer.write(request.method + "\n")
                writer.write(response.protocol.toString() + "\n")
                writer.write(response.code.toString() + "\n")
                writer.write(response.message + "\n")
                response.headers.names().forEach { header ->
                    writer.write(header + ": " + response.header(header) + "\n")
                }
            }
            val destination = File(offlineDirectory, File(path).name).path
            val cacheSource = CacheWritingSource(response.body.source(), File("$path.1").sink().buffer(),
                staged, destination, contentType.startsWith("image"), write, foregroundWrite)
            return response.newBuilder().body(CacheWritingResponseBody(cacheSource, contentType, contentLength)).build()
        } catch (e: IOException) {
            staged?.let { AppDatabase.instance.offlineObjectDao().deleteFilesForObject(it) }
            L.w(e)
            return response
        }
    }

    private inner class CacheWritingSource(private val source: BufferedSource, private val cacheSink: BufferedSink,
                                           private val staged: OfflineObject, private val destination: String,
                                           private val sharedImage: Boolean, private val write: OfflineCacheWrite?,
                                           private val foregroundWrite: OfflineCacheWrite?) : Source {
        private var caching = true

        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytesRead = try {
                source.read(sink, byteCount)
            } catch (e: IOException) {
                finish(publish = false)
                throw e
            }
            if (!caching) return bytesRead
            if (bytesRead == -1L) {
                finish(publish = true)
            } else {
                try {
                    sink.copyTo(cacheSink.buffer, sink.size - bytesRead, bytesRead)
                    cacheSink.emitCompleteSegments()
                } catch (e: IOException) {
                    L.w(e)
                    finish(publish = false)
                }
            }
            return bytesRead
        }

        private fun finish(publish: Boolean) {
            if (!caching) return
            caching = false
            val dao = AppDatabase.instance.offlineObjectDao()
            try {
                if (publish) {
                    cacheSink.close()
                    runBlocking {
                        var previousPaths: Set<String>? = null
                        try {
                            previousPaths = dao.publishResponse(staged, destination, write, sharedImage)
                        } finally {
                            dao.deleteUnreferencedFiles(previousPaths ?: setOf(destination))
                        }
                    }
                } else {
                    cacheSink.closeSilently()
                }
            } catch (e: Exception) {
                L.w(e)
            } finally {
                dao.deleteFilesForObject(staged)
                foregroundWrite?.close()
            }
        }

        override fun timeout(): Timeout = source.timeout()

        override fun close() {
            try {
                finish(publish = false)
            } finally {
                source.closeSilently()
            }
        }
    }

    private inner class CacheWritingResponseBody(source: Source,
                                                 private val contentType: String?,
                                                 private val contentLength: Long) : ResponseBody() {
        private val bufferedSource = source.buffer()

        override fun contentType(): MediaType? {
            return contentType?.toMediaTypeOrNull()
        }

        override fun contentLength(): Long {
            return contentLength
        }

        override fun source(): BufferedSource {
            return bufferedSource
        }
    }

    companion object {
        const val LANG_HEADER = "X-Offline-Lang"
        const val TITLE_HEADER = "X-Offline-Title"
        const val SAVE_HEADER = "X-Offline-Save"
        const val SAVE_HEADER_SAVE = "save"
        const val OFFLINE_PATH = "offline_files"
        private const val RESPONSE_FILE_PREFIX = "response-"

        private val responseStagingDirectory by lazy {
            // A rename preserves the source file's group. Stage beside the published files so
            // Android accounts for the response as app data after publication.
            File(WikipediaApp.instance.filesDir, "offline-responses").apply {
                // Initialization runs before any response in this process can start staging.
                listFiles().orEmpty().forEach { file ->
                    if (file.name.startsWith(RESPONSE_FILE_PREFIX) && (file.extension == "0" || file.extension == "1")) {
                        file.delete()
                    }
                }
            }
        }

        fun shouldSave(request: Request): Boolean {
            return "GET" == request.method && SAVE_HEADER_SAVE == request.header(SAVE_HEADER)
        }
    }
}

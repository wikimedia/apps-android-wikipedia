package org.wikipedia.yearinreview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import app.rive.ImageAsset
import app.rive.Result
import app.rive.core.RiveWorker
import app.rive.rememberImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.util.log.L
import java.io.IOException

// Maps each image property to its decoded image; null means the property is cleared, so the designer's sample picture never shows in its place
@Composable
fun rememberRiveImages(riveWorker: RiveWorker, imageUrls: Map<String, String?>): Map<String, Result<ImageAsset?>> {
    return imageUrls.mapValues { (property, url) ->
        key(property, url) {
            rememberRiveImage(riveWorker, url)
        }
    }
}

@Composable
private fun rememberRiveImage(riveWorker: RiveWorker, url: String?): Result<ImageAsset?> {
    if (url == null) {
        return Result.Success(null)
    }
    val bytesResult = produceState<Result<ByteArray?>>(initialValue = Result.Loading, key1 = url) {
        value = Result.Success(downloadImageBytes(url))
    }.value
    // Rive decodes the encoded file itself, which is why this downloads bytes rather than a Bitmap
    return bytesResult.andThen { bytes ->
        if (bytes == null) Result.Success(null) else rememberImage(riveWorker, bytes)
    }
}

private suspend fun downloadImageBytes(url: String): ByteArray? {
    return try {
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            OkHttpConnectionFactory.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code} for $url")
                }
                println("orange downloadImageBytes $url success")
                response.body.bytes()
            }
        }
    } catch (cancellationException: CancellationException) {
        throw cancellationException
    } catch (exception: Exception) {
        L.w(exception)
        null
    }
}

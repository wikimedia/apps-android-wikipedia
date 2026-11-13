package org.wikipedia.json

import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.wikipedia.dataclient.mwapi.MwException
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.lang.reflect.Type

/**
 * Wraps the kotlinx.serialization converter so that an MwException thrown while deserializing
 * a response (e.g. from MwResponse.init) reaches the caller directly, instead of wrapped in the
 * JsonDecodingException that kotlinx.serialization uses for exceptions thrown by deserializers.
 */
class MwExceptionConverterFactory : Converter.Factory() {
    private val delegate = JsonUtil.json.asConverterFactory("application/json".toMediaType())

    override fun responseBodyConverter(type: Type, annotations: Array<out Annotation>, retrofit: Retrofit): Converter<ResponseBody, *>? {
        val converter = delegate.responseBodyConverter(type, annotations, retrofit) ?: return null
        return Converter<ResponseBody, Any?> { body ->
            try {
                converter.convert(body)
            } catch (e: SerializationException) {
                throw e.cause as? MwException ?: e
            }
        }
    }

    override fun requestBodyConverter(type: Type, parameterAnnotations: Array<out Annotation>,
                                      methodAnnotations: Array<out Annotation>, retrofit: Retrofit): Converter<*, RequestBody>? {
        return delegate.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
    }
}

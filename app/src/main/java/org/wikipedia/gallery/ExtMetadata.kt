package org.wikipedia.gallery

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
class ExtMetadata(
    @SerialName("ObjectName") private val objectName: Values? = null,
    @SerialName("ImageDescription") private val imageDescription: Values? = null,
    @SerialName("DateTimeOriginal") private val dateTimeOriginal: Values? = null,
    @SerialName("Artist") private val artist: Values? = null,
    @SerialName("Credit") private val credit: Values? = null,
    @SerialName("LicenseShortName") private val licenseShortName: Values? = null,
    @SerialName("UsageTerms") private val usageTerms: Values? = null,
    @SerialName("LicenseUrl") private val licenseUrl: Values? = null,
    @SerialName("License") private val license: Values? = null,
) : Parcelable {

    fun licenseShortName(): String {
        return licenseShortName?.value.orEmpty()
    }

    fun licenseUrl(): String {
        return licenseUrl?.value.orEmpty()
    }

    fun license(): String {
        return license?.value.orEmpty()
    }

    fun imageDescription(): String {
        return imageDescription?.value.orEmpty()
    }

    fun imageDescriptionSource(): String {
        return imageDescription?.source.orEmpty()
    }

    fun objectName(): String {
        return objectName?.value.orEmpty()
    }

    fun usageTerms(): String {
        return usageTerms?.value.orEmpty()
    }

    fun dateTime(): String {
        return dateTimeOriginal?.value.orEmpty()
    }

    fun artist(): String {
        return artist?.value.orEmpty()
    }

    fun credit(): String {
        return credit?.value.orEmpty()
    }

    @Parcelize
    @Serializable
    class Values(
        val value: String? = null,
        val source: String? = null,
        val hidden: String? = null
    ) : Parcelable
}

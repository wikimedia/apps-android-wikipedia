package org.wikipedia.gallery

import com.google.gson.annotations.SerializedName

class ExtMetadata {

    @SerializedName("DateTime")
    private val dateTime: Values? = null

    @SerializedName("ObjectName")
    private val objectName: Values? = null

    @SerializedName("CommonsMetadataExtension")
    private val commonsMetadataExtension: Values? = null

    @SerializedName("Categories")
    private val categories: Values? = null

    @SerializedName("Assessments")
    private val assessments: Values? = null

    @SerializedName("ImageDescription")
    private val imageDescription: Values? = null

    @SerializedName("DateTimeOriginal")
    private val dateTimeOriginal: Values? = null

    @SerializedName("Artist")
    private val artist: Values? = null

    @SerializedName("Credit")
    private val credit: Values? = null

    @SerializedName("Permission")
    private val permission: Values? = null

    @SerializedName("AuthorCount")
    private val authorCount: Values? = null

    @SerializedName("LicenseShortName")
    private val licenseShortName: Values? = null

    @SerializedName("UsageTerms")
    private val usageTerms: Values? = null

    @SerializedName("LicenseUrl")
    private val licenseUrl: Values? = null

    @SerializedName("AttributionRequired")
    private val attributionRequired: Values? = null

    @SerializedName("Copyrighted")
    private val copyrighted: Values? = null

    @SerializedName("Restrictions")
    private val restrictions: Values? = null

    @SerializedName("License")
    private val license: Values? = null
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

    inner class Values {

        val value: String? = null
        val source: String? = null
        private val hidden: String? = null
    }
}

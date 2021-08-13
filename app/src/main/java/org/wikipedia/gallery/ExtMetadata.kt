package org.wikipedia.gallery

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
class ExtMetadata(
    @Json(name = "DateTime") internal val dateTimeValues: Values? = null,
    @Json(name = "ObjectName") internal val objectNameValues: Values? = null,
    @Json(name = "CommonsMetadataExtension") internal val commonsMetadataExtension: Values? = null,
    @Json(name = "Categories") internal val categories: Values? = null,
    @Json(name = "Assessments") internal val assessments: Values? = null,
    @Json(name = "ImageDescription") internal val imageDescriptionValues: Values? = null,
    @Json(name = "DateTimeOriginal") internal val dateTimeOriginalValues: Values? = null,
    @Json(name = "Artist") internal val artistValues: Values? = null,
    @Json(name = "Credit") internal val creditValues: Values? = null,
    @Json(name = "Permission") internal val permission: Values? = null,
    @Json(name = "AuthorCount") internal val authorCount: Values? = null,
    @Json(name = "LicenseShortName") internal val licenseShortNameValues: Values? = null,
    @Json(name = "UsageTerms") internal val usageTermsValues: Values? = null,
    @Json(name = "LicenseUrl") internal val licenseUrlValues: Values? = null,
    @Json(name = "AttributionRequired") internal val attributionRequired: Values? = null,
    @Json(name = "Copyrighted") internal val copyrighted: Values? = null,
    @Json(name = "Restrictions") internal val restrictions: Values? = null,
    @Json(name = "License") internal val licenseValues: Values? = null
) {
    val licenseShortName: String
        get() = licenseShortNameValues?.value ?: ""
    val licenseUrl: String
        get() = licenseUrlValues?.value ?: ""
    val license: String
        get() = licenseValues?.value ?: ""
    val imageDescription: String
        get() = imageDescriptionValues?.value ?: ""
    val imageDescriptionSource: String
        get() = imageDescriptionValues?.source ?: ""
    val objectName: String
        get() = objectNameValues?.value ?: ""
    val usageTerms: String
        get() = usageTermsValues?.value ?: ""
    val dateTime: String
        get() = dateTimeOriginalValues?.value ?: ""
    val artist: String
        get() = artistValues?.value ?: ""
    val credit: String
        get() = creditValues?.value ?: ""

    @JsonClass(generateAdapter = true)
    @Parcelize
    class Values(val value: String = "", val source: String = "", internal val hidden: String = "")
}

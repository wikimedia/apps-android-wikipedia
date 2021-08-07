package org.wikipedia.gallery

import androidx.annotation.DrawableRes
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.R
import java.io.Serializable
import java.util.*

@JsonClass(generateAdapter = true)
class ImageLicense(
    @Json(name = "type") val licenseName: String = "",
    @Json(name = "code") val licenseShortName: String = "",
    @Json(name = "url") val licenseUrl: String = ""
) : Serializable {
    constructor(metadata: ExtMetadata) : this(metadata.license, metadata.licenseShortName, metadata.licenseUrl)

    // Using the private modifier results in a compile error.
    internal val isLicenseCC: Boolean
        get() = licenseName.lowercase(Locale.ENGLISH).startsWith(CREATIVE_COMMONS_PREFIX) ||
                licenseShortName.lowercase(Locale.ENGLISH).startsWith(CREATIVE_COMMONS_PREFIX)
    internal val isLicensePD: Boolean
        get() = licenseName.lowercase(Locale.ENGLISH).startsWith(PUBLIC_DOMAIN_PREFIX) ||
                licenseShortName.lowercase(Locale.ENGLISH).startsWith(PUBLIC_DOMAIN_PREFIX)
    internal val isLicenseCCBySa: Boolean
        get() = licenseName.lowercase(Locale.ENGLISH).replace("-", "").startsWith(CC_BY_SA) ||
                licenseShortName.lowercase(Locale.ENGLISH).replace("-", "").startsWith(CC_BY_SA)

    /**
     * Return an icon (drawable resource id) that corresponds to the type of license
     * under which the specified Gallery item is provided.
     * @return Resource ID of the icon to display.
     */
    @get:DrawableRes
    val licenseIcon: Int
        get() = when {
            isLicensePD -> R.drawable.ic_license_pd
            isLicenseCCBySa -> R.drawable.ic_license_by
            isLicenseCC -> R.drawable.ic_license_cc
            else -> R.drawable.ic_license_cite
        }

    fun hasLicenseInfo(): Boolean {
        return !(licenseName.isEmpty() && licenseShortName.isEmpty() && licenseUrl.isEmpty())
    }

    companion object {
        private const val CREATIVE_COMMONS_PREFIX = "cc"
        private const val PUBLIC_DOMAIN_PREFIX = "pd"
        private const val CC_BY_SA = "ccbysa"
    }
}

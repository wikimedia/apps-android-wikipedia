package org.wikipedia.gallery

import androidx.annotation.DrawableRes
import com.google.gson.annotations.SerializedName
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import java.io.Serializable
import java.util.*

class ImageLicense : Serializable {

    @SerializedName("type") val licenseName: String

    @SerializedName("code") val licenseShortName: String

    @SerializedName("url") val licenseUrl: String

    constructor(metadata: ExtMetadata) {
        licenseName = metadata.license()
        licenseShortName = metadata.licenseShortName()
        licenseUrl = metadata.licenseUrl()
    }

    @JvmOverloads
    constructor(license: String = "", licenseShortName: String = "", licenseUrl: String = "") {
        licenseName = license
        this.licenseShortName = licenseShortName
        this.licenseUrl = licenseUrl
    }

    private val isLicenseCC: Boolean
        get() = (StringUtils.defaultString(licenseName).lowercase(Locale.ENGLISH).startsWith(CREATIVE_COMMONS_PREFIX) || StringUtils.defaultString(licenseShortName).toLowerCase(Locale.ENGLISH).startsWith(CREATIVE_COMMONS_PREFIX))
    private val isLicensePD: Boolean
        get() = (StringUtils.defaultString(licenseName).lowercase(Locale.ENGLISH).startsWith(PUBLIC_DOMAIN_PREFIX) || StringUtils.defaultString(licenseShortName).toLowerCase(Locale.ENGLISH).startsWith(PUBLIC_DOMAIN_PREFIX))
    private val isLicenseCCBySa: Boolean
        get() = (StringUtils.defaultString(licenseName).lowercase(Locale.ENGLISH).replace("-", "").startsWith(CC_BY_SA) || StringUtils.defaultString(licenseShortName).toLowerCase(Locale.ENGLISH).replace("-", "").startsWith(CC_BY_SA))

    /**
     * Return an icon (drawable resource id) that corresponds to the type of license
     * under which the specified Gallery item is provided.
     * @return Resource ID of the icon to display.
     */
    @get:DrawableRes
    val licenseIcon: Int
        get() {
            if (isLicensePD) {
                return R.drawable.ic_license_pd
            }
            if (isLicenseCCBySa) {
                return R.drawable.ic_license_by
            }
            return if (isLicenseCC) {
                R.drawable.ic_license_cc
            } else R.drawable.ic_license_cite
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

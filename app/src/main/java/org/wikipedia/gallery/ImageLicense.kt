package org.wikipedia.gallery

import androidx.annotation.DrawableRes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.R

@Serializable
class ImageLicense(
    @SerialName("type") var licenseName: String = "",
    @SerialName("code") var licenseShortName: String = "",
    @SerialName("url") var licenseUrl: String = ""
) {

    constructor(metadata: ExtMetadata) : this(metadata.license(), metadata.licenseShortName(), metadata.licenseUrl())

    private val isLicenseCC: Boolean
        get() = (licenseName.startsWith(CREATIVE_COMMONS_PREFIX, true) || licenseShortName.startsWith(CREATIVE_COMMONS_PREFIX, true))
    private val isLicensePD: Boolean
        get() = (licenseName.startsWith(PUBLIC_DOMAIN_PREFIX, true) || licenseShortName.startsWith(PUBLIC_DOMAIN_PREFIX, true))
    private val isLicenseCCBySa: Boolean
        get() = (licenseName.replace("-", "").startsWith(CC_BY_SA, true) || licenseShortName.replace("-", "").startsWith(CC_BY_SA, true))

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

    companion object {
        private const val CREATIVE_COMMONS_PREFIX = "cc"
        private const val PUBLIC_DOMAIN_PREFIX = "pd"
        private const val CC_BY_SA = "ccbysa"
    }
}

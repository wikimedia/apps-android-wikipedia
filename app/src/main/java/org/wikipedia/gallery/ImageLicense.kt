package org.wikipedia.gallery

import androidx.annotation.DrawableRes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.R
import java.util.*

@Serializable
class ImageLicense @JvmOverloads constructor(@SerialName("type") var licenseName: String = "",
                                             @SerialName("code") var licenseShortName: String = "",
                                             @SerialName("url") var licenseUrl: String = "") {

    constructor(metadata: ExtMetadata) : this(metadata.license(), metadata.licenseShortName(), metadata.licenseUrl())

    private val isLicenseCC: Boolean
        get() = (licenseName.lowercase(Locale.ENGLISH).startsWith(CREATIVE_COMMONS_PREFIX) || licenseShortName.lowercase(Locale.ENGLISH).startsWith(CREATIVE_COMMONS_PREFIX))
    private val isLicensePD: Boolean
        get() = (licenseName.lowercase(Locale.ENGLISH).startsWith(PUBLIC_DOMAIN_PREFIX) || licenseShortName.lowercase(Locale.ENGLISH).startsWith(PUBLIC_DOMAIN_PREFIX))
    private val isLicenseCCBySa: Boolean
        get() = (licenseName.lowercase(Locale.ENGLISH).replace("-", "").startsWith(CC_BY_SA) || licenseShortName.lowercase(Locale.ENGLISH).replace("-", "").startsWith(CC_BY_SA))

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

package org.wikipedia.gallery;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.R;

import java.io.Serializable;
import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class ImageLicense implements Serializable {
    private static final String CREATIVE_COMMONS_PREFIX = "cc";
    private static final String PUBLIC_DOMAIN_PREFIX = "pd";
    private static final String CC_BY_SA = "ccbysa";

    @NonNull @SerializedName("type") private final String license;
    @NonNull @SerializedName("code") private final String licenseShortName;
    @NonNull @SerializedName("url") private final String licenseUrl;

    public ImageLicense(@NonNull ExtMetadata metadata) {
        this.license = metadata.license();
        this.licenseShortName = metadata.licenseShortName();
        this.licenseUrl = metadata.licenseUrl();
    }

    public ImageLicense(@NonNull String license, @NonNull String licenseShortName, @NonNull String licenseUrl) {
        this.license = license;
        this.licenseShortName = licenseShortName;
        this.licenseUrl = licenseUrl;
    }

    public ImageLicense() {
        this("", "", "");
    }

    @NonNull public String getLicenseName() {
        return license;
    }

    @NonNull public String getLicenseShortName() {
        return licenseShortName;
    }

    @NonNull public String getLicenseUrl() {
        return licenseUrl;
    }

    private boolean isLicenseCC() {
        return defaultString(license).toLowerCase(Locale.ENGLISH).startsWith(CREATIVE_COMMONS_PREFIX)
                || defaultString(licenseShortName).toLowerCase(Locale.ENGLISH).startsWith(CREATIVE_COMMONS_PREFIX);
    }

    private boolean isLicensePD() {
        return defaultString(license).toLowerCase(Locale.ENGLISH).startsWith(PUBLIC_DOMAIN_PREFIX)
                || defaultString(licenseShortName).toLowerCase(Locale.ENGLISH).startsWith(PUBLIC_DOMAIN_PREFIX);
    }

    private boolean isLicenseCCBySa() {
        return defaultString(license).toLowerCase(Locale.ENGLISH).replace("-", "").startsWith(CC_BY_SA)
                || defaultString(licenseShortName).toLowerCase(Locale.ENGLISH).replace("-", "").startsWith(CC_BY_SA);
    }
    /**
     * Return an icon (drawable resource id) that corresponds to the type of license
     * under which the specified Gallery item is provided.
     * @return Resource ID of the icon to display.
     */
    @DrawableRes public int getLicenseIcon() {
        if (isLicensePD()) {
            return R.drawable.ic_license_pd;
        }
        if (isLicenseCCBySa()) {
            return R.drawable.ic_license_by;
        }
        if (isLicenseCC()) {
            return R.drawable.ic_license_cc;
        }
        return R.drawable.ic_license_cite;
    }

    public boolean hasLicenseInfo() {
        return !(license.isEmpty() && licenseShortName.isEmpty() && licenseUrl.isEmpty());
    }
}

package org.wikipedia.gallery;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import org.wikipedia.R;

import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class ImageLicense {
    private static final String CREATIVE_COMMONS_PREFIX = "cc";
    private static final String PUBLIC_DOMAIN_PREFIX = "pd";

    @NonNull private final String license;
    @NonNull private final String licenseShortName;
    @NonNull private final String licenseUrl;

    public ImageLicense(@NonNull ExtMetadata metadata) {
        this.license = metadata.license() != null ? metadata.license().value() : "";
        this.licenseShortName = metadata.licenseShortName() != null ? metadata.licenseShortName().value() : "";
        this.licenseUrl = metadata.licenseUrl() != null ? metadata.licenseUrl().value() : "";
    }

    public ImageLicense(@NonNull String license, @NonNull String licenseShortName, @NonNull String licenseUrl) {
        this.license = license;
        this.licenseShortName = licenseShortName;
        this.licenseUrl = licenseUrl;
    }

    public ImageLicense() {
        this("", "", "");
    }

    @NonNull public String getLicense() {
        return license;
    }

    @NonNull public String getLicenseShortName() {
        return licenseShortName;
    }

    @NonNull public String getLicenseUrl() {
        return licenseUrl;
    }

    public boolean isLicenseCC() {
        return defaultString(license).toLowerCase(Locale.ENGLISH).startsWith(CREATIVE_COMMONS_PREFIX)
                || defaultString(licenseShortName).toLowerCase(Locale.ENGLISH).startsWith(CREATIVE_COMMONS_PREFIX);
    }

    public boolean isLicensePD() {
        return defaultString(license).toLowerCase(Locale.ENGLISH).startsWith(PUBLIC_DOMAIN_PREFIX)
                || defaultString(licenseShortName).toLowerCase(Locale.ENGLISH).startsWith(PUBLIC_DOMAIN_PREFIX);
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
        if (isLicenseCC()) {
            return R.drawable.ic_license_cc;
        }
        return R.drawable.ic_license_cite;
    }

    public boolean hasLicenseInfo() {
        return !(license.isEmpty() && licenseShortName.isEmpty() && licenseUrl.isEmpty());
    }
}

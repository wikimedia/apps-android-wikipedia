package org.wikipedia.page;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.util.StringUtil;

public class ImageLicense {
    private static final String CREATIVE_COMMONS_PREFIX = "cc";
    private static final String PUBLIC_DOMAIN_PREFIX = "pd";

    @NonNull private final String license;
    @NonNull private final String licenseShortName;
    @NonNull private final String licenseUrl;

    public ImageLicense() {
        this("", "", "");
    }

    public ImageLicense(@NonNull String license, @NonNull String licenseShortName, @NonNull String licenseUrl) {
        this.license = license;
        this.licenseShortName = licenseShortName;
        this.licenseUrl = licenseUrl;
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
        return StringUtil.emptyIfNull(license).toLowerCase().startsWith(CREATIVE_COMMONS_PREFIX)
                || StringUtil.emptyIfNull(licenseShortName).toLowerCase().startsWith(CREATIVE_COMMONS_PREFIX);
    }

    public boolean isLicensePD() {
        return StringUtil.emptyIfNull(license).toLowerCase().startsWith(PUBLIC_DOMAIN_PREFIX)
                || StringUtil.emptyIfNull(licenseShortName).toLowerCase().startsWith(PUBLIC_DOMAIN_PREFIX);
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

package org.wikipedia.page;

import org.wikipedia.R;
import org.wikipedia.util.StringUtil;

public class ImageLicense {
    private static final String CREATIVE_COMMONS_PREFIX = "cc";
    private static final String PUBLIC_DOMAIN_PREFIX = "pd";

    private String license;
    private String licenseShortName;
    private String licenseUrl;

    public ImageLicense(String license, String licenseShortName, String licenseUrl) {
        this.license = license;
        this.licenseShortName = licenseShortName;
        this.licenseUrl = licenseUrl;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String licenseValue) {
        license = licenseValue;
    }

    public String getLicenseShortName() {
        return licenseShortName;
    }

    public void setLicenseShortName(String licenseShortNameValue) {
        licenseShortName = licenseShortNameValue;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrlValue) {
        licenseUrl = licenseUrlValue;
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
    public int getLicenseIcon() {
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

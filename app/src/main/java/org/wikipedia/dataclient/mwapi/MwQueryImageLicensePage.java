package org.wikipedia.dataclient.mwapi;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MwQueryImageLicensePage extends MwQueryPage {
    @SuppressWarnings("unused") @Nullable private List<ImageInfo> imageinfo;

    public String imageLicense() {
        return imageinfo != null
                && imageinfo.get(0).extmetadata() != null
                && imageinfo.get(0).extmetadata().license() != null
                ? imageinfo.get(0).extmetadata().license().value() : "";
    }

    public String imageLicenseShortName() {
        return imageinfo != null
                && imageinfo.get(0).extmetadata() != null
                && imageinfo.get(0).extmetadata().licenseShortName() != null
                ? imageinfo.get(0).extmetadata().licenseShortName().value() : "";
    }

    public String imageLicenseUrl() {
        return imageinfo != null
                && imageinfo.get(0).extmetadata() != null
                && imageinfo.get(0).extmetadata().licenseUrl() != null
                ? imageinfo.get(0).extmetadata().licenseUrl().value() : "";
    }

    static class ImageInfo {
        @SuppressWarnings("unused") private Extmetadata extmetadata;
        Extmetadata extmetadata() {
            return extmetadata;
        }
    }

    static class Extmetadata {
        @SuppressWarnings("unused") @SerializedName("License") private License license;
        @SuppressWarnings("unused") @SerializedName("LicenseShortName") private LicenseShortName licenseShortName;
        @SuppressWarnings("unused") @SerializedName("LicenseUrl") private LicenseUrl licenseUrl;

        License license() {
            return license;
        }
        LicenseShortName licenseShortName() {
            return licenseShortName;
        }
        LicenseUrl licenseUrl() {
            return licenseUrl;
        }
    }

    static class License {
        @SuppressWarnings("unused") private String value;
        String value() {
            return value;
        }
    }

    static class LicenseShortName {
        @SuppressWarnings("unused") private String value;
        String value() {
            return value;
        }
    }

    static class LicenseUrl {
        @SuppressWarnings("unused") private String value;
        String value() {
            return value;
        }
    }
}

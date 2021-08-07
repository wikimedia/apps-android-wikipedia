package org.wikipedia.gallery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("unused")
public class ExtMetadata {
    @SerializedName("DateTime") @Nullable private Values dateTime;
    @SerializedName("ObjectName") @Nullable private Values objectName;
    @SerializedName("CommonsMetadataExtension") @Nullable private Values commonsMetadataExtension;
    @SerializedName("Categories") @Nullable private Values categories;
    @SerializedName("Assessments") @Nullable private Values assessments;
    @SerializedName("ImageDescription") @Nullable private Values imageDescription;
    @SerializedName("DateTimeOriginal") @Nullable private Values dateTimeOriginal;
    @SerializedName("Artist") @Nullable private Values artist;
    @SerializedName("Credit") @Nullable private Values credit;
    @SerializedName("Permission") @Nullable private Values permission;
    @SerializedName("AuthorCount") @Nullable private Values authorCount;
    @SerializedName("LicenseShortName") @Nullable private Values licenseShortName;
    @SerializedName("UsageTerms") @Nullable private Values usageTerms;
    @SerializedName("LicenseUrl") @Nullable private Values licenseUrl;
    @SerializedName("AttributionRequired") @Nullable private Values attributionRequired;
    @SerializedName("Copyrighted") @Nullable private Values copyrighted;
    @SerializedName("Restrictions") @Nullable private Values restrictions;
    @SerializedName("License") @Nullable private Values license;

    @NonNull public String licenseShortName() {
        return StringUtils.defaultString(licenseShortName == null ? null : licenseShortName.value());
    }

    @NonNull public String licenseUrl() {
        return StringUtils.defaultString(licenseUrl == null ? null : licenseUrl.value());
    }

    @NonNull public String license() {
        return StringUtils.defaultString(license == null ? null : license.value());
    }

    @NonNull public String imageDescription() {
        return StringUtils.defaultString(imageDescription == null ? null : imageDescription.value());
    }

    @NonNull public String imageDescriptionSource() {
        return StringUtils.defaultString(imageDescription == null ? null : imageDescription.source());
    }

    @NonNull public String objectName() {
        return StringUtils.defaultString(objectName == null ? null : objectName.value());
    }

    @NonNull public String usageTerms() {
        return StringUtils.defaultString(usageTerms == null ? null : usageTerms.value());
    }

    @NonNull public String dateTime() {
        return StringUtils.defaultString(dateTimeOriginal == null ? null : dateTimeOriginal.value());
    }

    @NonNull public String artist() {
        return StringUtils.defaultString(artist == null ? null : artist.value());
    }

    @NonNull public String credit() {
        return StringUtils.defaultString(credit == null ? null : credit.value());
    }

    public class Values {
        @Nullable private String value;
        @Nullable private String source;
        @Nullable private String hidden;

        @Nullable public String value() {
            return value;
        }

        @Nullable public String source() {
            return source;
        }
    }
}

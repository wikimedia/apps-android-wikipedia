package org.wikipedia.gallery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class ExtMetadata {
    //Todo: only for testing - change after image captions logic is solidified
    void setDateTime(@Nullable Values dateTime) {
        this.dateTime = dateTime;
    }

    //Todo: only for testing - change after image captions logic is solidified
    void setImageDescription(@Nullable Values imageDescription) {
        this.imageDescription = imageDescription;
    }

    //Todo: only for testing - change after image captions logic is solidified
    public void setArtist(@Nullable Values artist) {
        this.artist = artist;
    }

    //Todo: only for testing - change after image captions logic is solidified
    void setLicenseShortName(@Nullable Values licenseShortName) {
        this.licenseShortName = licenseShortName;
    }

    @SuppressWarnings("unused") @SerializedName("DateTime") @Nullable private Values dateTime;
    @SuppressWarnings("unused") @SerializedName("ObjectName") @Nullable private Values objectName;
    @SuppressWarnings("unused") @SerializedName("CommonsMetadataExtension") @Nullable private Values commonsMetadataExtension;
    @SuppressWarnings("unused") @SerializedName("Categories") @Nullable private Values categories;
    @SuppressWarnings("unused") @SerializedName("Assessments") @Nullable private Values assessments;
    @SuppressWarnings("unused") @SerializedName("ImageDescription") @Nullable private Values imageDescription;
    @SuppressWarnings("unused") @SerializedName("DateTimeOriginal") @Nullable private Values dateTimeOriginal;
    @SuppressWarnings("unused") @SerializedName("Artist") @Nullable private Values artist;
    @SuppressWarnings("unused") @SerializedName("Credit") @Nullable private Values credit;
    @SuppressWarnings("unused") @SerializedName("Permission") @Nullable private Values permission;
    @SuppressWarnings("unused") @SerializedName("AuthorCount") @Nullable private Values authorCount;
    @SuppressWarnings("unused") @SerializedName("LicenseShortName") @Nullable private Values licenseShortName;
    @SuppressWarnings("unused") @SerializedName("UsageTerms") @Nullable private Values usageTerms;
    @SuppressWarnings("unused") @SerializedName("LicenseUrl") @Nullable private Values licenseUrl;
    @SuppressWarnings("unused") @SerializedName("AttributionRequired") @Nullable private Values attributionRequired;
    @SuppressWarnings("unused") @SerializedName("Copyrighted") @Nullable private Values copyrighted;
    @SuppressWarnings("unused") @SerializedName("Restrictions") @Nullable private Values restrictions;
    @SuppressWarnings("unused") @SerializedName("License") @Nullable private Values license;

    @Nullable public Values licenseShortName() {
        return licenseShortName;
    }

    @Nullable public Values licenseUrl() {
        return licenseUrl;
    }

    @Nullable public Values license() {
        return license;
    }

    @Nullable public Values imageDescription() {
        return imageDescription;
    }

    @Nullable public Values objectName() {
        return objectName;
    }

    @Nullable public Values usageTerms() {
        return usageTerms;
    }
    @Nullable public Values dateTime() {
        return dateTime;
    }

    @Nullable public Values artist() {
        return artist;
    }

    @Nullable public Values credit() {
        return credit;
    }

    public class Values {
        //Todo: only for testing - change after image captions logic is solidified
        public void setValue(@NonNull String value) {
            this.value = value;
        }

        //Todo: only for testing - change after image captions logic is solidified
        public void setSource(@NonNull String source) {
            this.source = source;
        }

        @SuppressWarnings("unused,NullableProblems") @NonNull private String value;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String source;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String hidden;

        @NonNull public String value() {
            return value;
        }

        @NonNull public String source() {
            return source;
        }
    }
}

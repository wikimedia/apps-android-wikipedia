package org.wikipedia.gallery;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ExtMetadata {
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

    public class Values {
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

    // TODO: Update consumers to use a ExtMetadata object and remove ASAP
    @Deprecated @NonNull Map<String, String> toMap() throws IllegalAccessException {
        HashMap<String, String> result = new HashMap<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            Values values = (Values) field.get(this);
            if (values != null) {
                result.put(StringUtils.capitalize(field.getName()), values.value());
            }
        }
        return result;
    }
}

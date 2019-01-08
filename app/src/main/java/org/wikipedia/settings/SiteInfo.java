package org.wikipedia.settings;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SiteInfo {
    @SuppressWarnings("unused") @Nullable private String mainpage;
    @SuppressWarnings("unused") @Nullable private String sitename;
    @SuppressWarnings("unused") @Nullable private String lang;
    @SuppressWarnings("unused") @Nullable private List<LanguageVariants> variants;
    @SuppressWarnings("unused") @SerializedName("readinglists-config")
    @Nullable private ReadingListsConfig readingListsConfig;

    @Nullable public String mainPage() {
        return mainpage;
    }

    @Nullable public ReadingListsConfig readingListsConfig() {
        return readingListsConfig;
    }

    @Nullable public String lang() {
        return lang;
    }

    public boolean hasVariants() {
        return variants != null && variants.size() > 0;
    }

    public static class ReadingListsConfig {
        @SuppressWarnings("unused") private int maxListsPerUser;
        @SuppressWarnings("unused") private int maxEntriesPerList;
        @SuppressWarnings("unused") private int deletedRetentionDays;

        public int maxEntriesPerList() {
            return maxEntriesPerList;
        }
    }

    private static class LanguageVariants {
        @SuppressWarnings("unused") private String code;
        @SuppressWarnings("unused") private String name;
    }
}

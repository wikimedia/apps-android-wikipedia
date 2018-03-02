package org.wikipedia.settings;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class SiteInfo {
    @SuppressWarnings("unused") @Nullable private String mainpage;
    @SuppressWarnings("unused") @Nullable private String sitename;
    @SuppressWarnings("unused") @Nullable private String lang;
    @SuppressWarnings("unused") @SerializedName("readinglists-config")
    @Nullable private ReadingListsConfig readingListsConfig;

    @Nullable public String mainPage() {
        return mainpage;
    }

    @Nullable public ReadingListsConfig readingListsConfig() {
        return readingListsConfig;
    }

    public static class ReadingListsConfig {
        @SuppressWarnings("unused") private int maxListsPerUser;
        @SuppressWarnings("unused") private int maxEntriesPerList;
        @SuppressWarnings("unused") private int deletedRetentionDays;

        public int maxEntriesPerList() {
            return maxEntriesPerList;
        }
    }
}

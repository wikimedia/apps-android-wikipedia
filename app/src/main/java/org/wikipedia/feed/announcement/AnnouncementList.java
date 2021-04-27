package org.wikipedia.feed.announcement;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class AnnouncementList {

    @SuppressWarnings("unused") @SerializedName("announce") @NonNull private List<Announcement> items = Collections.emptyList();

    @NonNull
    public List<Announcement> items() {
        return items;
    }

}

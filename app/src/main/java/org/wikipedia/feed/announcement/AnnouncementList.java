package org.wikipedia.feed.announcement;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.model.BaseModel;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

public class AnnouncementList extends BaseModel {

    @SuppressWarnings("unused") @SerializedName("announce") @NonNull private List<Announcement> items = Collections.emptyList();

    @NonNull
    List<Announcement> items() {
        return items;
    }

}

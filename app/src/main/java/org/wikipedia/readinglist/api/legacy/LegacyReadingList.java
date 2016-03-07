package org.wikipedia.readinglist.api.legacy;

import org.wikipedia.readinglist.api.ReadingList;

import com.google.gson.annotations.SerializedName;

import android.support.annotation.VisibleForTesting;

/**
 * Gson POJO for an API specific {@link ReadingList}.
 */
public class LegacyReadingList implements ReadingList {
    @VisibleForTesting int id;
    @VisibleForTesting String label;
    @VisibleForTesting String owner;
    @VisibleForTesting String perm;
    @VisibleForTesting String description;
    @VisibleForTesting String updated;
    @VisibleForTesting int count;
    @VisibleForTesting @SerializedName("imageurl") String imageUrl;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public String getOwner() {
        return owner;
    }

    public String getPerm() {
        return perm;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String getLastUpdated() {
        return updated;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public String getImageUrl() {
        return imageUrl;
    }
}

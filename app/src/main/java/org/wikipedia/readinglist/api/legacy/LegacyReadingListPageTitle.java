package org.wikipedia.readinglist.api.legacy;

import org.wikipedia.readinglist.api.ReadingListPageTitle;

import com.google.gson.annotations.SerializedName;

/**
 * Gson POJO for a member of a reading list.
 */
public class LegacyReadingListPageTitle implements ReadingListPageTitle {
    @SerializedName("ns")
    private int namespaceId;
    @SerializedName("title")
    private String prefixedTitle;

    public int getNamespaceId() {
        return namespaceId;
    }

    @Override
    public String getPrefixedTitle() {
        return prefixedTitle;
    }
}

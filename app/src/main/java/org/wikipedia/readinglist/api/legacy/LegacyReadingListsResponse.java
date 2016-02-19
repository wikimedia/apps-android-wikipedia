package org.wikipedia.readinglist.api.legacy;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import com.google.gson.annotations.SerializedName;

import android.support.annotation.VisibleForTesting;

import java.util.List;

/**
 * Gson POJO for a list of API specific reading lists.
 */
public class LegacyReadingListsResponse
        extends MwQueryResponse<LegacyReadingListsResponse.LegacyLists> {

    public static class LegacyLists {
        @VisibleForTesting @SerializedName("lists") List<LegacyReadingList> lists;

        public List<LegacyReadingList> getLists() {
            return lists;
        }
    }
}

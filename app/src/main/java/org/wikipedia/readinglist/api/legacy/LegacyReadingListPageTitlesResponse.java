package org.wikipedia.readinglist.api.legacy;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Gson POJO for the specific API response we get after requesting the list of page titles
 * contained in a given reading list.
 */
public class LegacyReadingListPageTitlesResponse
        extends MwQueryResponse<LegacyReadingListPageTitlesResponse.ListPages> {

    public static class ListPages {
        @SerializedName("listpages")
        private List<LegacyReadingListPageTitle> listPages;

        public List<LegacyReadingListPageTitle> getMemberPages() {
            return listPages;
        }
    }
}

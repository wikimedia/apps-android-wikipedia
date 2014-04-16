package org.wikipedia.pagehistory.usercontributions;

import android.content.*;
import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;
import org.wikipedia.pagehistory.*;

import java.util.*;

public class FetchUserContribsTask extends ApiTask<FetchUserContribsTask.UserContributionsList> {
    private final WikipediaApp app;
    private final Site site;
    private final String username;
    private final int numberToFetch;
    private final String queryContinue;

    public FetchUserContribsTask(Context context, Site site, String username, int numberToFetch, String queryContinue) {
        super(
                SINGLE_THREAD,
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(site)
        );
        app = (WikipediaApp)context.getApplicationContext();
        this.site = site;
        this.username = username;
        this.numberToFetch = numberToFetch;
        this.queryContinue = queryContinue;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        RequestBuilder builder = api.action("query")
                .param("list", "usercontribs")
                .param("uclimit", String.valueOf(numberToFetch))
                .param("ucuser", username)
                .param("ucprop", "title|timestamp|comment|sizediff");
        if (queryContinue != null) {
            builder.param("ucstart", queryContinue);
        }
        return builder;
    }

    @Override
    public UserContributionsList processResult(ApiResult result) throws Throwable {
        String continueString = null;
        if (result.asObject().has("query-continue")) {
            continueString = result.asObject().optJSONObject("query-continue").optJSONObject("usercontribs").optString("ucstart");
        }
        JSONArray contribsJSON = result.asObject().optJSONObject("query").optJSONArray("usercontribs");

        ArrayList<PageHistoryItem> contribs = new ArrayList<PageHistoryItem>(contribsJSON.length());

        for (int i = 0; i < contribsJSON.length(); i++) {
            JSONObject contribJSON = contribsJSON.optJSONObject(i);
            contribs.add(new PageHistoryItem(
                    contribJSON.optString("user"),
                    Utils.parseMWDate(contribJSON.optString("timestamp")),
                    contribJSON.optString("comment"),
                    contribJSON.optInt("sizeDiff"),
                    new PageTitle(contribJSON.optString("title"), site)
            ));
        }

        return new UserContributionsList(contribs, continueString);
    }

    public static class UserContributionsList {
        private final ArrayList<PageHistoryItem> contribs;
        private final String queryContinue;

        public UserContributionsList(ArrayList<PageHistoryItem> contribs, String queryContinue) {
            this.contribs = contribs;
            this.queryContinue = queryContinue;
        }

        public ArrayList<PageHistoryItem> getContribs() {
            return contribs;
        }

        public String getQueryContinue() {
            return queryContinue;
        }
    }
}

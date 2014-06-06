package org.wikipedia.editing;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.PageTitle;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;

public class DoEditTask extends ApiTask<EditingResult> {
    private final PageTitle title;
    private final String sectionWikitext;
    private final int sectionID;
    private final String summary;
    private final String editToken;
    private final WikipediaApp app;

    public DoEditTask(Context context, PageTitle title, String sectionWikitext, int sectionID, String editToken, String summary) {
        super(
                SINGLE_THREAD,
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(title.getSite())
        );
        this.title = title;
        this.sectionWikitext = sectionWikitext;
        this.sectionID = sectionID;
        this.editToken = editToken;
        this.summary = summary;
        this.app = (WikipediaApp)context.getApplicationContext();
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("edit")
                .param("title", title.getPrefixedText())
                .param("section", String.valueOf(sectionID))
                .param("text", sectionWikitext)
                .param("token", editToken)
                .param("summary", summary);
    }

    @Override
    protected ApiResult makeRequest(RequestBuilder builder) {
        return builder.post(); // Editing requires POST requests
    }

    @Override
    public EditingResult processResult(ApiResult result) throws Throwable {
        JSONObject resultJSON = result.asObject();

        if (WikipediaApp.isWikipediaZeroDevmodeOn()) {
            // TODO: ??? not sure if this is a better place for this or down below,
            // but the block seems to be ready for revisions, so clean up then?
            Utils.processHeadersForZero(app, result);
        }
        if (resultJSON.has("error")) {
            JSONObject errorJSON = resultJSON.optJSONObject("error");
            throw new EditingException(errorJSON.optString("code"), errorJSON.optString("info"));
        }
        JSONObject edit = resultJSON.optJSONObject("edit");
        String status = edit.optString("result");
        if (status.equals("Success")) {
            return new SuccessEditResult(edit.optInt("newrevid"));
        } else if (status.equals("Failure")) {
            if (edit.has("captcha")) {
                return new CaptchaResult(
                        edit.optJSONObject("captcha").optString("id")
                );
            }
            if (edit.has("code") && edit.optString("code").startsWith("abusefilter-")) {
                return new AbuseFilterEditResult(edit);
            }
            if (edit.has("spamblacklist")) {
                return new SpamBlacklistEditResult(edit.optString("spamblacklist"));
            }
        }
        // Handle other type of return codes here
        throw new RuntimeException("Failure happens");
    }
}

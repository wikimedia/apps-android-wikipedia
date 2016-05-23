package org.wikipedia.editing;

import android.content.Context;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.page.PageTitle;
import org.wikipedia.WikipediaApp;

import java.util.concurrent.TimeUnit;

public class EditTask extends ApiTask<EditingResult> {
    private final PageTitle title;
    private final String sectionWikitext;
    private final int sectionID;
    private final String summary;
    private final String editToken;
    private final boolean loggedIn;

    public EditTask(Context context, PageTitle title, String sectionWikitext, int sectionID,
                      String editToken, String summary, boolean loggedIn) {
        super(((WikipediaApp)context.getApplicationContext()).getAPIForSite(title.getSite()));
        this.title = title;
        this.sectionWikitext = sectionWikitext;
        this.sectionID = sectionID;
        this.editToken = editToken;
        this.summary = summary;
        this.loggedIn = loggedIn;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        RequestBuilder req = api.action("edit")
                .param("title", title.getPrefixedText())
                .param("section", String.valueOf(sectionID))
                .param("text", sectionWikitext)
                .param("token", editToken)
                .param("summary", summary);
        if (loggedIn) {
            // if the app believes that the user is logged in, then make sure to send an "assert"
            // parameter to the API, so that it will notify us if the user was actually logged
            // out on another device, and we'll need to log back in behind the scenes.
            req = req.param("assert", "user");
        }
        return req;
    }

    @Override
    protected ApiResult makeRequest(RequestBuilder builder) throws ApiException {
        return builder.post(); // Editing requires POST requests
    }

    @Override
    public EditingResult processResult(ApiResult result) throws Throwable {
        JSONObject resultJSON = result.asObject();
        JSONObject edit = resultJSON.optJSONObject("edit");
        String status = edit.optString("result");
        if (status.equals("Success")) {

            // TODO: remove when the server reflects the updated page content immediately
            // after submitting the edit, instead of a short while after.
            Thread.sleep(TimeUnit.SECONDS.toMillis(2));

            return new SuccessEditResult(edit.optInt("newrevid"));
        } else if (status.equals("Failure")) {
            if (edit.has("captcha")) {
                return new CaptchaResult(
                        edit.optJSONObject("captcha").optString("id")
                );
            }
            if (edit.has("code")) {
                return new AbuseFilterEditResult(edit);
            }
            if (edit.has("spamblacklist")) {
                return new SpamBlacklistEditResult(edit.optString("spamblacklist"));
            }
        }
        // Handle other type of return codes here
        throw new RuntimeException("Failure to recognise edit status: " + resultJSON.toString());
    }
}

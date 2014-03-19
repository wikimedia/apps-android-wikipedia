package org.wikipedia.zero;

import android.content.*;
import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;

public class WikipediaZeroTask extends ApiTask<String> {

    private Context ctx;

    public WikipediaZeroTask(Api api, Context context) {
        super(1, api);
        this.ctx = context;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("zeroconfig").param("type", "message").param("agent", Utils.getAppNameAndVersion(ctx));
    }

    @Override
    public String processResult(ApiResult result) throws Throwable {
        try {
            JSONObject results = result.asObject();
            String message = results.getString("message");
            return message;
        } catch (Exception e) {
            return null;
        }
    }
}
package org.wikipedia.wikidata;

import org.wikipedia.ApiTask;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.json.JSONObject;
import android.text.TextUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Retrieves descriptions for given language and IDs from Wikidata.
 */
public class WikidataDescriptionsTask extends ApiTask<Map<String, String>> {

    private final String language;
    private final List<String> idList;

    public WikidataDescriptionsTask(Api api, String language, List<String> idList) {
        super(LOW_CONCURRENCY, api);
        this.language = language;
        this.idList = idList;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("wbgetentities")
                  .param("props", "descriptions")
                  .param("languages", language)
                  .param("ids", TextUtils.join("|", idList));
    }

    @Override
    public Map<String, String> processResult(ApiResult result) throws Throwable {
        Map<String, String> map = new HashMap<String, String>();
        JSONObject data = result.asObject();
        JSONObject entities = data.getJSONObject("entities");

        Iterator<String> keys = entities.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject entity = entities.getJSONObject(key);
            JSONObject descriptions = entity.optJSONObject("descriptions");
            if (descriptions != null && descriptions.has(language)) {
                JSONObject langEntry = descriptions.getJSONObject(language);
                String value = langEntry.optString("value");
                //Capitalise the first letter of the description, for style
                value = value.substring(0, 1).toUpperCase() + value.substring(1);
                map.put(key, value);
            }
        }

        return map;
    }
}

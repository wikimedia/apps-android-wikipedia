package org.wikimedia.wikipedia;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class PageQueryTask<T> extends ApiTask<Map<PageTitle,T>> {
    private List<PageTitle> titles;

    public PageQueryTask(Context context, Site site, List<PageTitle> titles) {
        super(context, site);
        this.titles = titles;
    }

    @Override
    public ApiResult buildRequest(Api api) {
        RequestBuilder builder = api.action("query").param("titles", joinTitles());
        buildQueryParams(builder);
        return builder.get();
    }

    public String joinTitles() {
        StringBuilder str = new StringBuilder();
        boolean first = true;
        for (PageTitle title : titles) {
            if (first) {
                first = false;
            } else {
                str.append('|');
            }
            str.append(title.getPrefixedText());
        }
        Log.d("Wikipedia", "thumb list: " + str.toString());
        return str.toString();
    }

    @Override
    public Map<PageTitle,T> processResult(ApiResult result) throws Throwable {
        Map<PageTitle,T> map = new HashMap<PageTitle,T>();
        JSONObject data = result.asObject();
        JSONObject query = data.getJSONObject("query");
        JSONObject pages = query.getJSONObject("pages");
        Log.d("Wikipedia", "thumbs pages are: " + pages.toString());

        // You would think you could use foreach on an Iterator, but you can't.
        Iterator<String> keys = pages.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            int pageId = Integer.parseInt(key);
            JSONObject pageData = pages.getJSONObject(key);
            PageTitle pageTitle = processPageTitle(pageData);
            T pageResult = processPage(pageId, pageTitle, pageData);
            map.put(pageTitle, pageResult);
        }

        return map;
    }

    public PageTitle processPageTitle(JSONObject pageData) throws Throwable {
        String title = pageData.getString("title");
        return new PageTitle(null, title, getSite());
    }

    abstract public void buildQueryParams(RequestBuilder buildQueryParams);

    abstract public T processPage(int pageId, PageTitle pageTitle, JSONObject page) throws Throwable;
}

package org.wikipedia;

import android.text.*;
import org.json.*;
import org.mediawiki.api.json.*;

import java.util.*;

public abstract class PageQueryTask<T> extends ApiTask<Map<PageTitle,T>> {
    private final List<PageTitle> titles;
    private final Site site;

    public PageQueryTask(int threadCount, Api api, Site site, List<PageTitle> titles) {
        super(threadCount, api);
        this.titles = titles;
        this.site = site;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        RequestBuilder builder = api.action("query").param("titles", TextUtils.join("|", titles));
        buildQueryParams(builder);
        return builder;
    }

    @Override
    public Map<PageTitle,T> processResult(ApiResult result) throws Throwable {
        Map<PageTitle,T> map = new HashMap<PageTitle,T>();
        JSONObject data = result.asObject();
        JSONObject query = data.getJSONObject("query");
        JSONObject pages = query.getJSONObject("pages");

        // You would think you could use foreach on an Iterator, but you can't.
        Iterator<String> keys = pages.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            int pageId = Integer.parseInt(key);
            JSONObject pageData = pages.getJSONObject(key);
            String titleString = pageData.getString("title");
            PageTitle pageTitle = new PageTitle(null, titleString, site);
            T pageResult = processPage(pageId, pageTitle, pageData);
            map.put(pageTitle, pageResult);
        }

        return map;
    }

    abstract public void buildQueryParams(RequestBuilder buildQueryParams);

    abstract public T processPage(int pageId, PageTitle pageTitle, JSONObject page) throws Throwable;
}

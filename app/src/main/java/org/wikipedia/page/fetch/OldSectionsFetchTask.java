package org.wikipedia.page.fetch;

import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.Section;

import java.util.List;

/** For code that has not been moved to swappable page load mechanisms yet. */
public class OldSectionsFetchTask extends ApiTask<List<Section>> {
    private final SectionsFetcherPHP sectionsFetcher;

    public OldSectionsFetchTask(WikipediaApp app, PageTitle title, String sectionsRequested) {
        super(
                SINGLE_THREAD,
                app.getAPIForSite(title.getSite())
        );
        sectionsFetcher = new SectionsFetcherPHP(title, sectionsRequested,
                app.isImageDownloadEnabled());
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return sectionsFetcher.buildRequest(api);
    }

    @Override
    public List<Section> processResult(ApiResult result) throws Throwable {
        return sectionsFetcher.processResult(result);
    }
}

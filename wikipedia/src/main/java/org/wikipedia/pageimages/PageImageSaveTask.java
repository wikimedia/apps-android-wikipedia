package org.wikipedia.pageimages;

import org.mediawiki.api.json.Api;
import org.wikipedia.PageTitle;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.ExecutorService;

import java.util.Arrays;
import java.util.Map;

public class PageImageSaveTask extends PageImagesTask {
    private final WikipediaApp app;
    public PageImageSaveTask(WikipediaApp app, Api api, PageTitle title) {
        super(ExecutorService.getSingleton().getExecutor(PageImageSaveTask.class, 2), api, title.getSite(), Arrays.asList(new PageTitle[] {title}), 96);
        this.app = app;
    }

    @Override
    public void onFinish(Map<PageTitle, String> result) {
        for (Map.Entry<PageTitle, String> item : result.entrySet()) {
            PageImage pi = new PageImage(item.getKey(), item.getValue());
            app.getPersister(PageImage.class).upsert(pi);
        }
    }
}

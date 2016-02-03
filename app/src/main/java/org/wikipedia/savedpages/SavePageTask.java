package org.wikipedia.savedpages;

import org.wikipedia.database.DatabaseClient;
import org.wikipedia.page.PageTitle;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.page.Page;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/** Actual work to save a page for offline reading. */
public class SavePageTask extends SaneAsyncTask<Boolean> {
    private final WikipediaApp app;
    private final PageTitle title;
    private final Page page;

    private CountDownLatch imagesDownloadedLatch;

    public SavePageTask(WikipediaApp app, PageTitle title, Page page) {
        this.app = app;
        this.title = title;
        this.page = page;
    }

    @Override
    public Boolean performTask() throws Throwable {
        SavedPage savedPage = new SavedPage(title);
        savedPage.writeToFileSystem(page);
        DatabaseClient<SavedPage> client = app.getDatabaseClient(SavedPage.class);
        client.upsert(savedPage, SavedPageDatabaseTable.SELECTION_KEYS);

        final ImageUrlMap imageUrlMap = new ImageUrlMap.Builder(savedPage.getBaseDir()).extractUrls(page).build();
        final int numImagesAttempts = imageUrlMap.size();

        parallelDownload(imageUrlMap);

        savedPage.writeUrlMap(imageUrlMap.toJSON());
        return numImagesAttempts == imageUrlMap.size();
    }

    /**
     * Fans out image download to multiple threads so this is faster.
     * Borrows from SHA ab2676f4732f186696ce37d02fefa3e6aee13042.
     *
     * @param imageUrlMap a Map with entries {source URL, file path} of images to be downloaded
     * @throws InterruptedException
     */
    private void parallelDownload(final ImageUrlMap imageUrlMap) throws InterruptedException {
        imagesDownloadedLatch = new CountDownLatch(imageUrlMap.size());
        List<DownloadImageTask> tasks = new ArrayList<>();
        // instantiate the tasks first, then execute them all at once.
        // (so that removing URLs in onCatch doesn't mess with the iterator)
        for (Map.Entry<String, String> entry : imageUrlMap.entrySet()) {
            final String url = entry.getKey();
            final File file = new File(entry.getValue());
            tasks.add(new DownloadImageTask(app, url, file) {
                @Override
                public void onFinish(Boolean result) {
                    imagesDownloadedLatch.countDown();
                }

                @Override
                public void onCatch(Throwable caught) {
                    // TODO: Add retries
                    // An image failed to download, so exclude it from our URL Map
                    imageUrlMap.remove(url);
                    imagesDownloadedLatch.countDown();
                }
            });
        }
        for (DownloadImageTask task : tasks) {
            task.execute();
        }
        imagesDownloadedLatch.await();
    }
}

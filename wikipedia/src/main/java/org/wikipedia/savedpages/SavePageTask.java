package org.wikipedia.savedpages;

import android.content.Context;
import android.util.Log;
import com.github.kevinsawicki.http.HttpRequest;
import org.wikipedia.PageTitle;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.page.Page;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/** Actual work to save a page for offline reading. */
public class SavePageTask extends SaneAsyncTask<Void> {
    private static final String TAG = "SavePageTask";

    private final WikipediaApp app;
    private final PageTitle title;
    private final Page page;

    public SavePageTask(Context context, PageTitle title, Page page) {
        super(SINGLE_THREAD);
        app = (WikipediaApp) context.getApplicationContext();
        this.title = title;
        this.page = page;
    }

    @Override
    public Void performTask() throws Throwable {
        SavedPage savedPage = new SavedPage(title);
        savedPage.writeToFileSystem(page);
        SavedPagePersister persister = (SavedPagePersister) app.getPersister(SavedPage.class);
        persister.upsert(savedPage);

        ImageUrlMap imageUrlMap = new ImageUrlMap.Builder(savedPage.getBaseDir()).extractUrls(page).build();
        downloadImages(imageUrlMap);
        savedPage.writeUrlMap(imageUrlMap.toJSON());
        return null;
    }

    /**
     * Downloads all images in imageUrlMap.
     * imageUrlMap in the key and save to the file in the value of each entry in
     * @param imageUrlMap a Map with entries {source URL, file path} of images to be downloaded
     */
    private void downloadImages(ImageUrlMap imageUrlMap) {
        for (Map.Entry<String, String> entry : imageUrlMap.entrySet()) {
            String url = getFullUrl(entry.getKey());
            File file = new File(entry.getValue());
            try {
                downloadOne(url, file);
            } catch (IOException e) {
                Log.e(TAG, "could not write image to " + file.getAbsolutePath());
            }
        }
    }

    private void downloadOne(String url, File file) throws IOException {
        if (!url.startsWith("http")) {
            Log.e(TAG, "ignoring non-HTTP URL " + url);
            return;
        }
        HttpRequest request = HttpRequest.get(url).userAgent(app.getUserAgent());
        if (request.ok()) {
            writeFile(request.stream(), file);
            Log.d(TAG, "downloaded image " + url + " to " + file.getAbsolutePath());
        } else {
            Log.e(TAG, "could not download image " + url + " to " + file.getAbsolutePath());
        }
    }

    private void writeFile(InputStream inputStream, File file) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(file);
        try {
            Utils.copyStreams(inputStream, outputStream);
        } finally {
            outputStream.close();
        }
    }

    private String getFullUrl(String url) {
        String fullUrl;
        if (url.startsWith("//")) {
            // That's a protocol specific link! Make it https!
            fullUrl = WikipediaApp.PROTOCOL + ":" + url;
        } else {
            fullUrl = url;
        }
        return fullUrl;
    }
}

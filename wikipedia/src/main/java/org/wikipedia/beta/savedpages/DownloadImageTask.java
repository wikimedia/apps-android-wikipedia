package org.wikipedia.beta.savedpages;

import android.util.Log;
import com.github.kevinsawicki.http.HttpRequest;
import org.wikipedia.beta.Utils;
import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.concurrency.SaneAsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** */
public class DownloadImageTask extends SaneAsyncTask<Boolean> {
    private static final String TAG = "DownloadImageTask";

    private final WikipediaApp app;
    private final String imageUrl;
    private final File file;

    public DownloadImageTask(WikipediaApp app, String imageUrl, File file) {
        super(HIGH_CONCURRENCY);
        this.app = app;
        this.imageUrl = Utils.resolveProtocolRelativeUrl(imageUrl);
        this.file = file;
    }

    @Override
    public Boolean performTask() throws Throwable {
        try {
            return downloadOne(imageUrl, file);
        } catch (IOException e) {
            Log.e(TAG, "could not write image to " + file.getAbsolutePath());
            return false;
        }
    }

    private boolean downloadOne(String url, File file) throws IOException {
        if (!url.startsWith("http")) {
            Log.e(TAG, "ignoring non-HTTP URL " + url);
            return true;
        }
        HttpRequest request = HttpRequest.get(url).userAgent(app.getUserAgent());
        if (request.ok()) {
            writeFile(request.stream(), file);
            Log.d(TAG, "downloaded image " + url + " to " + file.getAbsolutePath());
            return true;
        } else {
            Log.e(TAG, "could not download image " + url + " to " + file.getAbsolutePath());
            return false;
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
}

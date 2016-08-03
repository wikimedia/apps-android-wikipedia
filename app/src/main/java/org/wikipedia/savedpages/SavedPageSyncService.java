package org.wikipedia.savedpages;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.WikipediaApp;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.page.ReadingListPageRow;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;
import org.wikipedia.readinglist.page.database.disk.ReadingListPageDiskRow;
import org.wikipedia.server.PageService;
import org.wikipedia.server.PageServiceFactory;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FileUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static org.wikipedia.util.FileUtil.writeFile;

public class SavedPageSyncService extends IntentService {
    @NonNull private ReadingListPageDao dao;

    public SavedPageSyncService() {
        super("SavedPageSyncService");
        dao = ReadingListPageDao.instance();
    }

    @Override
    protected void onHandleIntent(@NonNull Intent intent) {
        if (!DeviceUtil.isOnline(this)) {
            L.i("Device is offline; aborting sync service");
            return;
        }

        List<ReadingListPageDiskRow> queue = new ArrayList<>();
        Collection<ReadingListPageDiskRow> rows = dao.startDiskTransaction();
        L.i("Syncing saved rlp pages with saved pages service");

        for (final ReadingListPageDiskRow row : rows) {
            L.v("Found pending tx with status: " + row.status().name());
            switch (row.status()) {
                case UNSAVED:
                case DELETED:
                    String filename = row.filename();
                    if (filename != null) {
                        FileUtil.delete(new File(filename), true);
                        dao.completeDiskTransaction(row);
                        L.v("Deleted" + filename);
                        continue;
                    }
                    L.e("Found row with null filename; skipping");
                    continue;
                case OUTDATED:
                    queue.add(row);
                    continue;
                case ONLINE:
                case SAVED:
                    L.w("Received row with unexpected status " + row.status() + ": "
                            + row.toString());
                    continue;
                default:
                    throw new UnsupportedOperationException("Invalid disk row status: "
                            + row.status().name());
            }
        }
        saveNewEntries(queue);
    }

    @VisibleForTesting
    public void saveNewEntries(List<ReadingListPageDiskRow> queue) {
        while (!queue.isEmpty()) {
            ReadingListPageDiskRow row = queue.get(0);
            boolean ok = savePageFor(row);
            if (!ok) {
                dao.failDiskTransaction(queue);
                break;
            }
            dao.completeDiskTransaction(row);
            queue.remove(row);
        }
    }

    @VisibleForTesting
    public boolean savePageFor(@NonNull ReadingListPageDiskRow row) {
        final PageTitle title = makeTitleFrom(row);
        if (title == null) {
            return false;
        }

        try {
            final Page page = getApiService(title).pageCombo(title.getPrefixedText(),
                            !WikipediaApp.getInstance().isImageDownloadEnabled()).toPage(title);
            final SavedPage savedPage = new SavedPage(page.getTitle());
            final ImageUrlMap imageUrlMap = new ImageUrlMap.Builder(FileUtil.getSavedPageDirFor(title))
                .extractUrls(page).build();
            savedPage.writeToFileSystem(page);
            downloadImages(imageUrlMap);
            savedPage.writeUrlMap(imageUrlMap.toJSON());
            L.i("Page " + title.getDisplayText() + " saved!");
            return true;
        } catch (Exception e) {
            L.e("Failed to save page " + title.getDisplayText(), e);
            return false;
        }
    }

    @Nullable
    private PageTitle makeTitleFrom(@NonNull ReadingListPageDiskRow row) {
        ReadingListPageRow pageRow = row.dat();
        if (pageRow == null) {
            return null;
        }
        String namespace = pageRow.namespace().toLegacyString();
        return new PageTitle(namespace, pageRow.title(), pageRow.site());
    }

    /**
     * @param imageUrlMap a Map with entries {source URL, file path} of images to be downloaded
     */
    private void downloadImages(@NonNull final ImageUrlMap imageUrlMap) {
        for (Map.Entry<String, String> entry : imageUrlMap.entrySet()) {
            final String url = UriUtil.resolveProtocolRelativeUrl(entry.getKey());
            final File file = new File(entry.getValue());
            try {
                downloadImage(url, file);
            } catch (IOException e) {
                L.e("Failed to download image: " + url, e);
            }
        }
    }

    private boolean downloadImage(@NonNull String url, @NonNull File file) throws IOException {
        if (!url.startsWith("http")) {
            L.e("ignoring non-HTTP URL " + url);
            return true;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url)
                                               .header("User-Agent", WikipediaApp.getInstance().getUserAgent())
                                               .build();
        Response response = client.newCall(request).execute();

        try {
            if (response.isSuccessful()) {
                InputStream stream = response.body().byteStream();
                writeFile(stream, file);
                L.v("downloaded image " + url + " to " + file.getAbsolutePath());
                return true;
            }
        } catch (Exception e) {
            L.e("could not download image " + url, e);
        } finally {
            response.close();
        }
        return false;
    }

    @NonNull
    private PageService getApiService(@NonNull PageTitle title) {
        return PageServiceFactory.create(title.getSite());
    }
}

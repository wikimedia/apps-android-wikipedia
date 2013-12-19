package org.wikimedia.wikipedia.savedpages;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wikimedia.wikipedia.CommunicationBridge;
import org.wikimedia.wikipedia.Page;
import org.wikimedia.wikipedia.WikipediaApp;
import org.wikimedia.wikipedia.concurrency.ExecutorService;
import org.wikimedia.wikipedia.concurrency.SaneAsyncTask;

import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

public class SavePageTask extends SaneAsyncTask<Void> {
    private final WikipediaApp app;
    private final Page page;
    private final CommunicationBridge bridge;

    /**
     * Latch that opens when the imagesDownloadedLatch has been initialized.
     * This is needed because the count of the imagesDownloadedLatch needs to
     * be asynchronously determined, and will be null until that can be
     * determined. So we have another single counter latch that is just to check
     * for the imagesDownloadedLatch being initialized
     */
    private CountDownLatch imagesDownloadLatchInitialized;
    private CountDownLatch imagesDownloadedLatch;

    public SavePageTask(Context context, CommunicationBridge bridge, Page page) {
        super(ExecutorService.getSingleton().getExecutor(SavePageTask.class, 1));
        app = (WikipediaApp) context.getApplicationContext();
        this.page = page;
        this.bridge = bridge;

        imagesDownloadLatchInitialized = new CountDownLatch(1);
    }

    @Override
    public Void performTask() throws Throwable {
        SavedPagePerister persister = (SavedPagePerister) app.getPersister(SavedPage.class);

        persister.savePageContent(page);

        final CommunicationBridge.JSEventListener processImages = new CommunicationBridge.JSEventListener() {
            // This runs on the main thread
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                final JSONArray imagesList = messagePayload.optJSONArray("images");
                final HashSet<String> hashSet = new HashSet<String>();
                imagesDownloadedLatch = new CountDownLatch(imagesList.length());
                imagesDownloadLatchInitialized.countDown();
                for (int i = 0; i < imagesList.length(); i++) {
                    final String imageUrl = imagesList.optString(i);
                    if (!hashSet.contains(imageUrl)) {
                        hashSet.add(imageUrl);
                        new DownloadImageTask(app, imageUrl) {
                            @Override
                            public void onFinish(Boolean result) {
                                imagesDownloadedLatch.countDown();
                                Log.d("Wikipedia", "Downloaded image " + imageUrl);
                            }
                        }.execute();
                    }
                }
            }
        };

        Handler mainThreadHandler = new Handler(app.getMainLooper(), new Handler.Callback() {
            // This also runs on the main thread - you can access the bridge only from the Main thread
            @Override
            public boolean handleMessage(Message msg) {
                bridge.addListener("imagesListResponse", processImages);
                bridge.sendMessage("requestImagesList", new JSONObject());
                return true;
            }
        });

        mainThreadHandler.sendEmptyMessage(0);

        imagesDownloadLatchInitialized.await();
        imagesDownloadedLatch.await();

        SavedPage savedPage = new SavedPage(page.getTitle());

        persister.upsert(savedPage);
        return null;
    }
}

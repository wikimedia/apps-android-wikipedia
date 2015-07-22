package org.wikipedia.random;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import org.wikipedia.page.PageTitle;
import org.wikipedia.WikipediaApp;

public class RandomHandler {
    private WikipediaApp app;
    private RandomArticleIdTask curRandomArticleIdTask;
    private static final int MESSAGE_RND = 1;

    private MenuItem randomMenuItem;
    private boolean isClosed;
    private RandomListener randomListener;

    public interface RandomListener {
        void onRandomPageReceived(PageTitle title);
        void onRandomPageFailed(Throwable caught);
    }

    public RandomHandler(MenuItem menuItem, RandomListener listener) {
        randomMenuItem = menuItem;
        randomListener = listener;
        this.app = WikipediaApp.getInstance();
        isClosed = false;

        //set initial state...
        setState(false);
    }

    private void setState(boolean busy) {
        randomMenuItem.setEnabled(!busy);
    }

    public void onStop() {
        isClosed = true;
    }

    public void doVisitRandomArticle() {
        Handler randomHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                RandomArticleIdTask randomTask = new RandomArticleIdTask(app.getAPIForSite(app.getPrimarySite()), app.getPrimarySite()) {

                    @Override
                    public void onBeforeExecute() {
                        setState(true);
                    }

                    @Override
                    public void onFinish(PageTitle title) {
                        if (isClosed) {
                            return;
                        }
                        setState(false);
                        Log.d("Wikipedia", "Random article title pulled: " + title);
                        randomListener.onRandomPageReceived(title);
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        if (isClosed) {
                            return;
                        }
                        setState(false);
                        Log.d("Wikipedia", "Random article ID retrieval failed");
                        curRandomArticleIdTask = null;
                        randomListener.onRandomPageFailed(caught);
                    }
                };
                if (curRandomArticleIdTask != null) {
                    // if this connection was hung, clean up a bit
                    curRandomArticleIdTask.cancel();
                }
                curRandomArticleIdTask = randomTask;
                curRandomArticleIdTask.execute();
                return true;
            }
        });
        randomHandler.removeMessages(MESSAGE_RND);
        Message randomMessage = Message.obtain();
        randomMessage.what = MESSAGE_RND;
        randomMessage.obj = "random";
        randomHandler.sendMessage(randomMessage);
    }
}

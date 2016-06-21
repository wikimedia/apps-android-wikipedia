package org.wikipedia.random;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.wikipedia.MainActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.WikipediaApp;

public class RandomHandler {
    private WikipediaApp app;
    private RandomArticleIdTask curRandomArticleIdTask;
    private static final int MESSAGE_RND = 1;

    private MainActivity activity;
    private RandomListener listener;

    public interface RandomListener {
        void onRandomPageReceived(PageTitle title);
        void onRandomPageFailed(Throwable caught);
    }

    public RandomHandler(MainActivity activity, RandomListener listener) {
        this.activity = activity;
        this.listener = listener;
        this.app = WikipediaApp.getInstance();

        //set initial state...
        setProgressBarLoading(false);
        setNavMenuItemEnabled(true);
    }

    private void setNavMenuItemEnabled(boolean enabled) {
        activity.setNavMenuItemRandomEnabled(enabled);
    }

    private void setProgressBarLoading(boolean loading) {
        activity.updateProgressBar(loading, true, 0);
    }

    public void doVisitRandomArticle() {
        Handler randomHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                RandomArticleIdTask randomTask = new RandomArticleIdTask(app.getAPIForSite(app.getSite()), app.getSite()) {

                    @Override
                    public void onBeforeExecute() {
                        setProgressBarLoading(true);
                        setNavMenuItemEnabled(false);
                    }

                    @Override
                    public void onFinish(PageTitle title) {
                        setProgressBarLoading(false);
                        setNavMenuItemEnabled(true);
                        Log.d("Wikipedia", "Random article title pulled: " + title);
                        listener.onRandomPageReceived(title);
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        setProgressBarLoading(false);
                        setNavMenuItemEnabled(true);
                        Log.d("Wikipedia", "Random article ID retrieval failed");
                        curRandomArticleIdTask = null;
                        listener.onRandomPageFailed(caught);
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

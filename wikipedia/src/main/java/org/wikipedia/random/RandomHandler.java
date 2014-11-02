package org.wikipedia.random;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

public class RandomHandler {
    private WikipediaApp app;
    private RandomArticleIdTask curRandomArticleIdTask;
    private static final int MESSAGE_RND = 1;

    private View randomMenuItem;
    private View randomIcon;
    private View randomProgressBar;
    private boolean isClosed;
    private RandomListener randomListener;

    public interface RandomListener {
        void onRandomPageReceived(PageTitle title);
    }

    public RandomHandler(View menuItem, View icon, View progressBar, RandomListener listener) {
        randomMenuItem = menuItem;
        randomIcon = icon;
        randomProgressBar = progressBar;
        randomListener = listener;
        this.app = WikipediaApp.getInstance();
        isClosed = false;

        //set initial state...
        setState(false);
    }

    private void setState(boolean busy) {
        randomMenuItem.setEnabled(!busy);
        randomProgressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        randomIcon.setVisibility(busy ? View.GONE : View.VISIBLE);
    }

    public void onStop() {
        isClosed = true;
    }

    public void doVisitRandomArticle() {
        Handler randomHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                RandomArticleIdTask randomTask = new RandomArticleIdTask(app.getAPIForSite(app.getPrimarySite()), app.getPrimarySite(), app) {

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
                        if (title != null) {
                            randomListener.onRandomPageReceived(title);
                        } else {
                            // Rather than close the menubar and lose the current page...
                            Toast.makeText(app, app.getString(R.string.error_network_error), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        if (isClosed) {
                            return;
                        }
                        setState(false);
                        Log.d("Wikipedia", "Random article ID retrieval failed");
                        curRandomArticleIdTask = null;
                        Toast.makeText(app, app.getString(R.string.error_network_error), Toast.LENGTH_LONG).show();
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

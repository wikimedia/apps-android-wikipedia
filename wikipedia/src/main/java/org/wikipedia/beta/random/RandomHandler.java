package org.wikipedia.beta.random;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.squareup.otto.Bus;
import org.wikipedia.beta.PageTitle;
import org.wikipedia.beta.R;
import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.events.NewWikiPageNavigationEvent;
import org.wikipedia.beta.history.HistoryEntry;

public class RandomHandler {
    private WikipediaApp app;
    private RandomArticleIdTask curRandomArticleIdTask;
    private static final int MESSAGE_RND = 1;

    private View randomMenuItem;
    private View randomIcon;
    private View randomProgressBar;
    private boolean isClosed;

    public RandomHandler(View menuItem, View icon, View progressBar) {
        randomMenuItem = menuItem;
        randomIcon = icon;
        randomProgressBar = progressBar;
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
        final Bus bus = app.getBus();
        Handler randomHandler = new Handler(new Handler.Callback(){
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
                            HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_RANDOM);
                            bus.post(new NewWikiPageNavigationEvent(title, historyEntry));
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

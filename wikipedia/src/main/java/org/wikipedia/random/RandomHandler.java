package org.wikipedia.random;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import com.squareup.otto.Bus;
import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.events.NewWikiPageNavigationEvent;
import org.wikipedia.history.HistoryEntry;

public class RandomHandler {
    private Activity activity;
    private WikipediaApp app;
    private RandomArticleIdTask curRandomArticleIdTask;
    private static final int MESSAGE_RND = 1;

    public RandomHandler(Activity activity) {
        this.activity = activity;
        this.app = (WikipediaApp)(activity.getApplicationContext());
    }

    public void doVistRandomArticle() {
        final Bus bus = app.getBus();
        Handler randomHandler = new Handler(new Handler.Callback(){
            @Override
            public boolean handleMessage(Message msg) {
                RandomArticleIdTask randomTask = new RandomArticleIdTask(app.getAPIForSite(app.getPrimarySite()), app.getPrimarySite(), app) {
                    private ProgressDialog progressDialog;

                    @Override
                    public void onBeforeExecute() {
                        progressDialog = new ProgressDialog(activity);
                        progressDialog.setMessage(activity.getString(R.string.random_progress));
                        progressDialog.setIndeterminate(true);
                        progressDialog.setCanceledOnTouchOutside(false); // require back button to abort
                        progressDialog.show();
                        progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                cancel();
                            }
                        });
                    }

                    @Override
                    public void onFinish(PageTitle title) {
                        progressDialog.dismiss();
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
                        // oh snap
                        progressDialog.dismiss();
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

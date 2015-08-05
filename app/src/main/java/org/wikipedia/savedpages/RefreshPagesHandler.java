package org.wikipedia.savedpages;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.Section;

import java.util.List;

/**
 * Handler for refreshing a list of Saved Pages
 */
public class RefreshPagesHandler {
    /**
     * Variable set to true if the refresh in progress has been cancelled
     */
    private boolean isRefreshCancelled = false;
    /**
     * Variable used to track number of  saved pages that have been completed so far
     */
    private int savedPagesCompleted = 0;
    private final List<SavedPage> savedPages;
    private final Context context;
    private ProgressDialog progressDialog;

    public RefreshPagesHandler(Context context, List<SavedPage> savedPages) {
        this.savedPages = savedPages;
        this.context = context;

        progressDialog = new ProgressDialog(context);
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(savedPages.size());
        progressDialog.setProgress(0);
        progressDialog.setMessage(context.getResources().getString(R.string.saved_pages_progress_title));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isRefreshCancelled = true;
            }
        });
    }

    public void onStop() {
        if (progressDialog.isShowing()) {
            isRefreshCancelled = true;
            progressDialog.dismiss();
        }
    }

    /**
     * Start refreshing the saved pages.
     *
     * This function returns after starting the refresh and does not block
     */
    public void refresh() {
        // Reset flags
        isRefreshCancelled = false;
        savedPagesCompleted = 0;
        progressDialog.show();
        for (int i = 0; i < savedPages.size(); i++) {
            final SavedPage savedPage = savedPages.get(i);
                new RefreshSavedPageTask(WikipediaApp.getInstance(), savedPage.getTitle()) {
                    @Override
                    public void onBeforeExecute() {
                        if (isRefreshCancelled) {
                            cancel();
                            return;
                        }
                        Log.d("Wikipedia", "refreshing start: " + savedPage.getTitle().getDisplayText());
                    }

                    @Override
                    public void onFinish(List<Section> result) {
                        if (!progressDialog.isShowing()) {
                            isRefreshCancelled = true;
                            // no longer attached to activity!
                            return;
                        }
                        savedPagesCompleted++;
                        progressDialog.setProgress(savedPagesCompleted);
                        Log.d("Wikipedia", "Count is " + savedPagesCompleted + " of " + savedPages.size());
                        if (savedPagesCompleted == savedPages.size()) {
                            progressDialog.dismiss();
                        }
                        Log.d("Wikipedia", "refreshing end: " + savedPage.getTitle().getDisplayText());
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        isRefreshCancelled = true;
                        if (!progressDialog.isShowing()) {
                            // no longer attached to activity!
                            return;
                        }
                        progressDialog.dismiss();
                        getErrorDialog().show();
                    }
                }.execute();
        }
    }

    private AlertDialog errorDialog;

    /**
     * Returns a persistant dialog we can use to show errors.
     * @return A properly setup AlertDialog with handlers for retry and cancel.
     */
    private AlertDialog getErrorDialog() {
        if (errorDialog == null) {
            errorDialog = new AlertDialog.Builder(context)
                    .setPositiveButton(R.string.saved_pages_update_all_error_retry, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            refresh();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .setMessage(R.string.saved_pages_update_all_error_message)
                    .show();
        }
        return errorDialog;
    }
}

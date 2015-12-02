package org.wikipedia.savedpages;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;
import org.wikipedia.server.PageService;
import org.wikipedia.server.ContentServiceFactory;
import org.wikipedia.util.log.L;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

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
        for (int i = 0; i < savedPages.size() && !isRefreshCancelled; i++) {
            final SavedPage savedPage = savedPages.get(i);
            L.d("refreshing start: " + savedPage.getTitle().getDisplayText());
            refreshOneSavedPage(savedPage.getTitle());
        }
    }

    private void refreshOneSavedPage(@NonNull final PageTitle title) {
        getApiService(title).pageCombo(title.getPrefixedText(),
                !WikipediaApp.getInstance().isImageDownloadEnabled(),
                new SaveOtherPageCallback(title) {
                    @Override
                    protected void onComplete() {
                        if (!progressDialog.isShowing()) {
                            isRefreshCancelled = true;
                            // no longer attached to activity!
                            return;
                        }
                        savedPagesCompleted++;
                        progressDialog.setProgress(savedPagesCompleted);
                        L.d("Count is " + savedPagesCompleted + " of " + savedPages.size());
                        if (savedPagesCompleted == savedPages.size()) {
                            progressDialog.dismiss();
                        }
                    }

                    @Override
                    protected void onError() {
                        isRefreshCancelled = true;
                        if (!progressDialog.isShowing()) {
                            // no longer attached to activity!
                            return;
                        }
                        progressDialog.dismiss();
                        getErrorDialog().show();
                    }
                });
    }

    private PageService getApiService(PageTitle title) {
        return ContentServiceFactory.create(title.getSite());
    }

    private AlertDialog errorDialog;

    /**
     * Returns a persistent dialog we can use to show errors.
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

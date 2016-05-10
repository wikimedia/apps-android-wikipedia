package org.wikipedia.savedpages;

import org.wikipedia.WikipediaApp;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageTitle;
import org.wikipedia.server.PageCombo;
import org.wikipedia.util.log.L;

/**
 * Common code for saving a page which is not the current page.
 * Useful for refreshing a saved page, or for saving a page from a link, search result, disambig
 * entry or similar.
 */
public abstract class SaveOtherPageCallback implements PageCombo.Callback {
    private final PageTitle title;

    public SaveOtherPageCallback(PageTitle title) {
        this.title = title;
    }

    @Override
    public void success(PageCombo pageCombo) {
        if (pageCombo.hasError()) {
            onError();
            return;
        }

        final Page page = pageCombo.toPage(title);
        new SavePageTask(WikipediaApp.getInstance(), page.getTitle(), page) {
            @Override
            public void onFinish(Boolean result) {
                L.d("Downloaded page " + title.getDisplayText());
                onComplete();
            }
        }.execute();
    }

    @Override
    public void failure(Throwable throwable) {
        L.e("Download page error: " + throwable);
        onError();
    }

    protected abstract void onComplete();

    protected abstract void onError();
}
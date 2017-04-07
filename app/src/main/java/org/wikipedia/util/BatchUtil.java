package org.wikipedia.util;

import android.support.annotation.NonNull;

import org.wikipedia.page.PageTitle;

import java.util.List;

import static org.wikipedia.Constants.API_QUERY_MAX_TITLES;

public final class BatchUtil {
    // Useful for API requests in which we want a number of results that exceeds the limit for at
    // least one of the modules being used in the query (e.g., 50 for PageImages).
    //
    // TODO: This function does not yet handle batchcomplete.  For requests using a generator
    // together with properties, the API result may signal to continue because there are more
    // properties to retrieve for the pages so far, or because there are more pages from the
    // generator, or both.
    //
    // https://www.mediawiki.org/wiki/API:Query#batchcomplete
    //
    // Implement continuation/batchcomplete handling if we want to batch requests for a query in
    // which we are using a generator.
    //
    // Bug: T162497
    public static <T> void makeBatches(@NonNull final List<PageTitle> titles,
                                       @NonNull Handler<T> handler,
                                       @NonNull final Callback<T> callback) {
        for (int i = 0; i < titles.size(); i += Math.min(API_QUERY_MAX_TITLES, titles.size() - i)) {
            handler.handleBatch(titles.subList(i, i + Math.min(API_QUERY_MAX_TITLES, titles.size() - i)),
                    titles.size(), new Callback<T>() {
                @Override
                public void success(@NonNull List<T> results) {
                    callback.success(results);
                }

                @Override
                public void failure(@NonNull Throwable caught) {
                    callback.failure(caught);
                }
            });
        }
    }

    public interface Handler<T> {
        void handleBatch(@NonNull List<PageTitle> batchTitles, int total, Callback<T> cb);
    }

    public interface Callback<T> {
        void success(@NonNull List<T> results);
        void failure(@NonNull Throwable caught);
    }

    private BatchUtil() {
    }
}

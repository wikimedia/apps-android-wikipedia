package org.wikipedia.feed.becauseyouread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.model.Card;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.bottomcontent.MainPageReadMoreTopicTask;
import org.wikipedia.search.FullTextSearchClient;
import org.wikipedia.search.SearchResult;
import org.wikipedia.search.SearchResults;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;

import static org.wikipedia.Constants.SUGGESTION_REQUEST_ITEMS;

public class BecauseYouReadClient extends FullTextSearchClient implements FeedClient {
    @Nullable private MainPageReadMoreTopicTask readMoreTopicTask;
    @Nullable private Call<MwQueryResponse> fullTextSearchCall;

    @Override public void request(@NonNull Context context, @NonNull final WikiSite wiki, int age,
                                  @NonNull final FeedClient.Callback cb) {
        cancel();
        readMoreTopicTask = new MainPageReadMoreTopicTask(age) {
            @Override public void onFinish(@Nullable HistoryEntry entry) {
                if (entry == null) {
                    cb.success(Collections.emptyList());
                    return;
                }
                getCardForHistoryEntry(entry, cb);
            }

            @Override public void onCatch(Throwable caught) {
                L.e("Error fetching 'because you read' suggestions", caught);
                cb.error(caught);
            }
        };
        readMoreTopicTask.execute();
    }

    @Override public void cancel() {
        if (readMoreTopicTask != null) {
            readMoreTopicTask.cancel();
            readMoreTopicTask = null;
        }
        if (fullTextSearchCall != null) {
            fullTextSearchCall.cancel();
            fullTextSearchCall = null;
        }
    }

    private void getCardForHistoryEntry(@NonNull final HistoryEntry entry,
                                        final FeedClient.Callback cb) {
        requestMoreLike(entry.getTitle().getWikiSite(), entry.getTitle().getDisplayText(),
                null, null, SUGGESTION_REQUEST_ITEMS, new FullTextSearchClient.Callback() {
            @Override public void success(@NonNull Call<MwQueryResponse> call,
                                          @NonNull SearchResults results) {
                SearchResults filteredResults = SearchResults
                        .filter(results, entry.getTitle().getText(), false);
                cb.success(filteredResults.getResults().isEmpty()
                        ? Collections.<Card>emptyList()
                        : Collections.singletonList(toBecauseYouReadCard(results, entry)));
            }

            @Override public void failure(@NonNull Call<MwQueryResponse> call,
                                          @NonNull Throwable caught) {
                cb.error(caught);
            }
        });
    }

    @NonNull private BecauseYouReadCard toBecauseYouReadCard(@NonNull SearchResults results,
                                                             @NonNull HistoryEntry entry) {
        List<BecauseYouReadItemCard> itemCards = new ArrayList<>();
        for (SearchResult result : results.getResults()) {
            itemCards.add(new BecauseYouReadItemCard(result.getPageTitle()));
        }
        return new BecauseYouReadCard(entry, itemCards);
    }
}

package org.wikipedia.feed.becauseyouread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.Constants;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.restbase.RbRelatedPages;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.feed.FeedCoordinator;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.bottomcontent.MainPageReadMoreTopicTask;
import org.wikipedia.search.RelatedPagesSearchClient;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;

public class BecauseYouReadClient implements FeedClient {
    @Nullable private MainPageReadMoreTopicTask readMoreTopicTask;

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
    }

    private void getCardForHistoryEntry(@NonNull final HistoryEntry entry,
                                        final FeedClient.Callback cb) {

        new RelatedPagesSearchClient().request(entry.getTitle().getConvertedText(), entry.getTitle().getWikiSite(),
                Constants.SUGGESTION_REQUEST_ITEMS, new RelatedPagesSearchClient.Callback() {
            @Override
            public void success(@NonNull Call<RbRelatedPages> call, @Nullable List<RbPageSummary> results) {
                FeedCoordinator.postCardsToCallback(cb, (results == null || results.size() == 0)
                        ? Collections.emptyList()
                        : Collections.singletonList(toBecauseYouReadCard(results, entry)));
            }

            @Override
            public void failure(@NonNull Call<RbRelatedPages> call, @NonNull Throwable caught) {
                cb.error(caught);
            }
        });
    }

    @NonNull private BecauseYouReadCard toBecauseYouReadCard(@NonNull List<RbPageSummary> results,
                                                             @NonNull HistoryEntry entry) {
        List<BecauseYouReadItemCard> itemCards = new ArrayList<>();
        for (RbPageSummary result : results) {
            itemCards.add(new BecauseYouReadItemCard(result.getPageTitle(entry.getTitle().getWikiSite())));
        }
        return new BecauseYouReadCard(entry, itemCards);
    }
}

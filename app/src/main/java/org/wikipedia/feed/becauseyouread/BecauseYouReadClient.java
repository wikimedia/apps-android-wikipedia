package org.wikipedia.feed.becauseyouread;

import android.content.Context;

import androidx.annotation.NonNull;

import org.wikipedia.Constants;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.feed.FeedCoordinator;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.history.HistoryEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class BecauseYouReadClient implements FeedClient {
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override public void request(@NonNull Context context, @NonNull final WikiSite wiki, int age,
                                  @NonNull final FeedClient.Callback cb) {
        cancel();
        disposables.add(Observable.fromCallable(new MainPageReadMoreTopicTask(age))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(entry -> getCardForHistoryEntry(entry, cb),
                        throwable -> cb.success(Collections.emptyList())));
    }

    @Override public void cancel() {
        disposables.clear();
    }

    private void getCardForHistoryEntry(@NonNull final HistoryEntry entry,
                                        final FeedClient.Callback cb) {

        disposables.add(ServiceFactory.getRest(entry.getTitle().getWikiSite()).getRelatedPages(entry.getTitle().getPrefixedText())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(response -> response.getPages(Constants.SUGGESTION_REQUEST_ITEMS))
                .subscribe(results -> FeedCoordinator.postCardsToCallback(cb, (results == null || results.size() == 0)
                            ? Collections.emptyList() : Collections.singletonList(toBecauseYouReadCard(results, entry))),
                        cb::error));
    }

    @NonNull private BecauseYouReadCard toBecauseYouReadCard(@NonNull List<PageSummary> results,
                                                             @NonNull HistoryEntry entry) {
        List<BecauseYouReadItemCard> itemCards = new ArrayList<>();
        for (PageSummary result : results) {
            itemCards.add(new BecauseYouReadItemCard(result.getPageTitle(entry.getTitle().getWikiSite())));
        }
        return new BecauseYouReadCard(entry, itemCards);
    }
}

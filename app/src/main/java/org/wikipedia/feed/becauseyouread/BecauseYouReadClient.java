package org.wikipedia.feed.becauseyouread;

import android.content.Context;
import android.util.Pair;

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
        disposables.add(Observable.zip(ServiceFactory.getRest(entry.getTitle().getWikiSite()).getSummary(entry.getReferrer(), entry.getTitle().getPrefixedText()),
                ServiceFactory.getRest(entry.getTitle().getWikiSite()).getRelatedPages(entry.getTitle().getPrefixedText()), Pair::new)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(pair -> new Pair<>(pair.first, pair.second.getPages(Constants.SUGGESTION_REQUEST_ITEMS)))
                .subscribe(pair -> FeedCoordinator.postCardsToCallback(cb, (pair.second == null || pair.second.size() == 0)
                            ? Collections.emptyList() : Collections.singletonList(toBecauseYouReadCard(pair.second, pair.first, entry.getTitle().getWikiSite()))),
                        cb::error));
    }

    @NonNull private BecauseYouReadCard toBecauseYouReadCard(@NonNull List<PageSummary> results,
                                                             @NonNull PageSummary pageSummary,
                                                             @NonNull WikiSite wikiSite) {
        List<BecauseYouReadItemCard> itemCards = new ArrayList<>();
        for (PageSummary result : results) {
            itemCards.add(new BecauseYouReadItemCard(result.getPageTitle(wikiSite)));
        }
        return new BecauseYouReadCard(pageSummary.getPageTitle(wikiSite), itemCards);
    }
}

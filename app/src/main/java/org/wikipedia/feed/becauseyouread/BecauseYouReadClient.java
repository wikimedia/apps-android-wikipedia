package org.wikipedia.feed.becauseyouread;

import android.content.Context;

import androidx.annotation.NonNull;

import org.wikipedia.Constants;
import org.wikipedia.WikipediaApp;
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

        // If the language code has a parent language code, it means set "Accept-Language" will slow down the loading time of /page/related
        // TODO: remove when https://phabricator.wikimedia.org/T271145 is resolved.
        boolean shouldSetLanguageHeader = WikipediaApp.getInstance().language().getDefaultLanguageCode(entry.getTitle().getWikiSite().languageCode()) == null;

        disposables.add(ServiceFactory.getRest(entry.getTitle().getWikiSite(), shouldSetLanguageHeader).getRelatedPages(entry.getTitle().getPrefixedText())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(relatedPages -> {
                    List<PageSummary> list = relatedPages.getPages(Constants.SUGGESTION_REQUEST_ITEMS);
                    list.add(0, new PageSummary(entry.getTitle().getDisplayText(), entry.getTitle().getPrefixedText(), entry.getTitle().getDescription(),
                            entry.getTitle().getExtract(), entry.getTitle().getThumbUrl(), entry.getTitle().getWikiSite().languageCode()));
                    return Observable.fromIterable(list);
                })
                .concatMap(pageSummary -> shouldSetLanguageHeader
                        ? Observable.just(pageSummary) : ServiceFactory.getRest(entry.getTitle().getWikiSite()).getSummary(entry.getReferrer(), pageSummary.getApiTitle()))
                .observeOn(AndroidSchedulers.mainThread())
                .toList()
                .subscribe(list -> {
                    PageSummary headerPage = list.remove(0);
                    FeedCoordinator.postCardsToCallback(cb, (list == null || list.size() == 0)
                                    ? Collections.emptyList() : Collections.singletonList(toBecauseYouReadCard(list, headerPage, entry.getTitle().getWikiSite())));
                }, cb::error));
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

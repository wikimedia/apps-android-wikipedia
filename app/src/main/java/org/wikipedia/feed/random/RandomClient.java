package org.wikipedia.feed.random;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.feed.FeedContentType;
import org.wikipedia.feed.FeedCoordinator;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.log.L;

import java.util.Collections;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RandomClient implements FeedClient {

    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public void request(@NonNull Context context, @NonNull WikiSite wiki, int age, @NonNull Callback cb) {
        cancel();
        disposables.add(Observable.fromIterable(FeedContentType.getAggregatedLanguages())
                .flatMap(this::getRandomSummaryObservable, Pair::new)
                .observeOn(AndroidSchedulers.mainThread())
                .toList()
                .subscribe(pairs -> {
                    for (Pair<String, PageSummary> pair : pairs) {
                        if (pair.first != null && pair.second != null) {
                            FeedCoordinator.postCardsToCallback(cb, Collections.singletonList(new RandomCard(pair.second, age, WikiSite.forLanguageCode(pair.first))));
                        }
                    }
                }, t -> {
                    L.v(t);
                    cb.error(t);
                }));

    }

    private Observable<PageSummary> getRandomSummaryObservable(@NonNull String lang) {
        return ServiceFactory.getRest(WikiSite.forLanguageCode(lang))
                .getRandomSummary()
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(throwable -> Observable.fromCallable(() -> {
                    ReadingListPage page = ReadingListDbHelper.INSTANCE.getRandomPage();
                    if (page == null) {
                        throw (Exception) throwable;
                    }
                    return ReadingListPage.toPageSummary(page);
                }));
    }

    @Override
    public void cancel() {
        disposables.clear();
    }
}

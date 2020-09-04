package org.wikipedia.feed.random;

import android.content.Context;

import androidx.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
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
        disposables.add(ServiceFactory.getRest(WikipediaApp.getInstance().getWikiSite()).getRandomSummary()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(throwable -> Observable.fromCallable(() -> {
                            ReadingListPage page = ReadingListDbHelper.instance().getRandomPage();
                            if (page == null) {
                                throw (Exception) throwable;
                            }
                            return ReadingListPage.toPageSummary(page);
                        }
                ))
                .subscribe(pageSummary -> FeedCoordinator.postCardsToCallback(cb, Collections.singletonList(new RandomCard(pageSummary, age, wiki))), t -> {
                    L.v(t);
                    cb.error(t);
                }));

    }

    @Override
    public void cancel() {
        disposables.clear();
    }
}

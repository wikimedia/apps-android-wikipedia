package org.wikipedia.feed.random;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.feed.view.StaticCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class RandomCardView extends StaticCardView<RandomCard> {
    public interface Callback {
        void onRandomClick(@NonNull RandomCardView view);
        void onGetRandomError(@NonNull Throwable t, @NonNull RandomCardView view);
    }

    public RandomCardView(@NonNull Context context) {
        super(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTransitionName(getString(R.string.transition_random_activity));
        }
    }

    @Override public void setCard(@NonNull RandomCard card) {
        super.setCard(card);
        setTitle(getString(R.string.view_random_card_title));
        setSubtitle(getString(R.string.view_random_card_subtitle));
        setIcon(R.drawable.ic_casino_accent50_24dp);
        setContainerBackground(R.color.accent50);
        setAction(R.drawable.ic_casino_accent50_24dp, R.string.view_random_card_action);
    }

    protected void onContentClick(View v) {
        if (getCallback() != null) {
            getCallback().onRandomClick(RandomCardView.this);
        }
    }

    protected void onActionClick(View v) {
        if (getCallback() != null) {
            getCallback().onRandomClick(RandomCardView.this);
        }
    }

    @SuppressLint("CheckResult")
    public void getRandomPage() {
        setProgress(true);
        ServiceFactory.getRest(WikipediaApp.getInstance().getWikiSite()).getRandomSummary()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(pageSummary -> new PageTitle(null, pageSummary.getTitle(), WikipediaApp.getInstance().getWikiSite()))
                .onErrorResumeNext(throwable -> {
                    return Observable.fromCallable(() -> {
                                ReadingListPage page = ReadingListDbHelper.instance().getRandomPage();
                                if (page == null) {
                                    throw (Exception) throwable;
                                }
                                return ReadingListPage.toPageTitle(page);
                            }
                    );
                })
                .doFinally(() -> setProgress(false))
                .subscribe(pageTitle -> {
                    if (getCallback() != null && getCard() != null) {
                        getCallback().onSelectPage(getCard(),
                                new HistoryEntry(pageTitle, HistoryEntry.SOURCE_FEED_RANDOM));
                    }
                }, t -> {
                    if (getCallback() != null && getCard() != null) {
                        getCallback().onGetRandomError(t, RandomCardView.this);
                    }
                });
    }
}

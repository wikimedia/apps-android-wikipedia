package org.wikipedia.page;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.util.log.L;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

final class PageCacher {

    @SuppressLint("CheckResult")
    static void loadIntoCache(@NonNull PageTitle title) {
        L.d("Loading page into cache: " + title.getPrefixedText());

        ServiceFactory.getRest(title.getWikiSite())
                .getSummaryResponse(title.getPrefixedText(), null, null, null, null, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(summaryRsp -> {
                    for (int i = WikipediaApp.getInstance().getTabCount() - 1; i >= 0; i--) {
                        PageTitle pageTitle = WikipediaApp.getInstance().getTabList().get(i).getBackStackPositionTitle();
                        if (pageTitle.equals(title)) {
                            pageTitle.setThumbUrl(summaryRsp.body().getThumbnailUrl());
                            break;
                        }
                    }
                }, L::e);
    }

    private PageCacher() { }
}

package org.wikipedia.page;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.util.log.L;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;

final class PageCacher {

    @SuppressLint("CheckResult")
    static void loadIntoCache(@NonNull PageTitle title) {
        L.d("Loading page into cache: " + title.getPrefixedText());
        Observable.zip(leadReq(title), remainingReq(title), (leadRsp, sectionsRsp) -> leadRsp)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(leadRsp -> {
                    for (int i = WikipediaApp.getInstance().getTabCount() - 1; i >= 0; i--) {
                        PageTitle pageTitle = WikipediaApp.getInstance().getTabList().get(i).getBackStackPositionTitle();
                        if (pageTitle.equals(title)) {
                            pageTitle.setThumbUrl(leadRsp.body().getThumbUrl());
                            break;
                        }
                    }
                }, L::e);
    }

    @NonNull
    private static Observable<Response<PageLead>> leadReq(@NonNull PageTitle title) {
        return ServiceFactory.getRest(title.getWikiSite()).getLeadSection(null, null, null, title.getPrefixedText());
    }

    @NonNull
    private static Observable<Response<PageRemaining>> remainingReq(@NonNull PageTitle title) {
        return ServiceFactory.getRest(title.getWikiSite()).getRemainingSections(null, null, title.getPrefixedText());
    }

    private PageCacher() { }
}

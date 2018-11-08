package org.wikipedia.page;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageClientFactory;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.util.log.L;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;

import static org.wikipedia.util.DimenUtil.calculateLeadImageWidth;

final class PageCacher {

    @SuppressLint("CheckResult")
    static void loadIntoCache(@NonNull PageTitle title) {
        L.d("Loading page into cache: " + title.getPrefixedText());
        PageClient client = PageClientFactory.create(title.getWikiSite(), title.namespace());
        Observable.zip(leadReq(client, title), remainingReq(client, title), (leadRsp, sectionsRsp) -> true)
                .subscribeOn(Schedulers.io())
                .subscribe(val -> { }, L::e);
    }

    @NonNull
    private static Observable<Response<PageLead>> leadReq(@NonNull PageClient client, @NonNull PageTitle title) {
        return client.lead(title.getWikiSite(), null, null, null, title.getPrefixedText(),
                calculateLeadImageWidth());
    }

    @NonNull
    private static Observable<Response<PageRemaining>> remainingReq(@NonNull PageClient client,
                                                    @NonNull PageTitle title) {
        return client.sections(title.getWikiSite(), null, null, title.getPrefixedText());
    }

    private PageCacher() { }
}

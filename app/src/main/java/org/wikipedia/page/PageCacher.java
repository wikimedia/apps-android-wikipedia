package org.wikipedia.page;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.RestService;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Request;
import retrofit2.Response;

final class PageCacher {

    @SuppressLint("CheckResult")
    static void loadIntoCache(@NonNull PageTitle title) {
        L.d("Loading page into cache: " + title.getPrefixedText());

        Observable.zip(summaryReq(title), mobileHtmlReq(title), (summaryRsp, mobileHtmlRsp) -> summaryRsp)
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

    private static Observable<okhttp3.Response> mobileHtmlReq(@NonNull PageTitle pageTitle) {
        Request request = new Request.Builder().url(UriUtil.resolveProtocolRelativeUrl(pageTitle.getWikiSite(),
                pageTitle.getWikiSite().url() + RestService.REST_API_PREFIX + RestService.PAGE_HTML_ENDPOINT + pageTitle.getPrefixedText()))
                .addHeader("Accept-Language", WikipediaApp.getInstance().getAcceptLanguage(pageTitle.getWikiSite()))
                .build();

        return Observable.create(emitter -> {
            okhttp3.Response response = OkHttpConnectionFactory.getClient().newCall(request).execute();
            emitter.onNext(response);
            emitter.onComplete();
        });
    }

    @NonNull
    private static Observable<Response<PageSummary>> summaryReq(@NonNull PageTitle title) {
        return ServiceFactory.getRest(title.getWikiSite())
                .getSummaryResponse(title.getPrefixedText(), null, null, null, null, null);
    }

    private PageCacher() { }
}

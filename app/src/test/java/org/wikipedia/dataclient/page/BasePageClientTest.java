package org.wikipedia.dataclient.page;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.observers.TestObserver;
import okhttp3.CacheControl;
import retrofit2.Response;

import static org.wikipedia.dataclient.Service.PREFERRED_THUMB_SIZE;

public abstract class BasePageClientTest extends MockRetrofitTest {
    @Test public void testLeadCacheControl() throws Throwable {
        TestObserver<Response<PageLead>> observer = new TestObserver<>();
        subject().lead(wikiSite(), CacheControl.FORCE_NETWORK, null, null, "foo", 0).subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().header("Cache-Control").contains("no-cache"));
    }

    @Test public void testLeadNoCacheControl() throws Throwable {
        TestObserver<Response<PageLead>> observer = new TestObserver<>();
        subject().lead(wikiSite(), null, null, null, "foo", 0).subscribe(observer);
        observer.awaitTerminalEvent();
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().header("Cache-Control").contains("max-stale=0"));
    }

    @Test public void testLeadHttpRefererUrl() throws Throwable {
        String refererUrl = "https://en.wikipedia.org/wiki/United_States";
        TestObserver<Response<PageLead>> observer = new TestObserver<>();
        subject().lead(wikiSite(), null, null, refererUrl, "foo", 0).subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().header("Referer").contains(refererUrl));
    }

    @Test public void testLeadCacheOptionCache() throws Throwable {
        TestObserver<Response<PageLead>> observer = new TestObserver<>();
        subject().lead(wikiSite(), null, null, null, "foo", 0).subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().header(OfflineCacheInterceptor.SAVE_HEADER) == null);
    }

    @Test public void testLeadCacheOptionSave() throws Throwable {
        TestObserver<Response<PageLead>> observer = new TestObserver<>();
        subject().lead(wikiSite(), null, OfflineCacheInterceptor.SAVE_HEADER_SAVE, null, "foo", 0).subscribe(observer);
        observer.assertComplete().assertValue(result -> result.raw().request().header(OfflineCacheInterceptor.SAVE_HEADER).contains(OfflineCacheInterceptor.SAVE_HEADER_SAVE));
    }

    @Test public void testLeadTitle() throws Throwable {
        TestObserver<Response<PageLead>> observer = new TestObserver<>();
        subject().lead(wikiSite(), null, null, null, "Title", 0).subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> {
                    System.out.println(result.raw().request().url());
                    System.out.println(result.raw().request().url().toString());
                    return result.raw().request().url().toString().contains("Title");
                });
    }

    @Test public void testSectionsCacheControl() throws Throwable {
        TestObserver<Response<PageRemaining>> observer = new TestObserver<>();
        subject().sections(wikiSite(), CacheControl.FORCE_NETWORK, null, "foo").subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().header("Cache-Control").contains("no-cache"));
    }

    @Test public void testSectionsNoCacheControl() throws Throwable {
        TestObserver<Response<PageRemaining>> observer = new TestObserver<>();
        subject().sections(wikiSite(), null, null, "foo").subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().header("Cache-Control").contains("max-stale=0"));
    }

    @Test public void testSectionsCacheOptionCache() throws Throwable {
        TestObserver<Response<PageRemaining>> observer = new TestObserver<>();
        subject().sections(wikiSite(), null, null, "foo").subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().header(OfflineCacheInterceptor.SAVE_HEADER) == null);
    }

    @Test public void testSectionsCacheOptionSave() throws Throwable {
        TestObserver<Response<PageRemaining>> observer = new TestObserver<>();
        subject().sections(wikiSite(), null, OfflineCacheInterceptor.SAVE_HEADER_SAVE,  "foo").subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().header(OfflineCacheInterceptor.SAVE_HEADER).contains(OfflineCacheInterceptor.SAVE_HEADER_SAVE));
    }

    @Test public void testSectionsTitle() throws Throwable {
        TestObserver<Response<PageRemaining>> observer = new TestObserver<>();
        subject().sections(wikiSite(), null, null, "Title").subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().url().toString().contains("Title"));
    }

    @NonNull protected abstract PageClient subject();

    protected String preferredThumbSizeString() {
        return Integer.toString(PREFERRED_THUMB_SIZE) + "px";
    }
}

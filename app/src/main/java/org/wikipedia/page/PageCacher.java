package org.wikipedia.page;

import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageClientFactory;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.html.ImageTagParser;
import org.wikipedia.html.PixelDensityDescriptorParser;
import org.wikipedia.savedpages.PageImageUrlParser;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;
import static org.wikipedia.util.DimenUtil.calculateLeadImageWidth;
import static org.wikipedia.util.UriUtil.resolveProtocolRelativeUrl;

public final class PageCacher {

    public static void loadIntoCache(@NonNull PageTitle title) {
        L.d("Loading page into cache: " + title.getPrefixedText());
        WikipediaApp app = WikipediaApp.getInstance();
        PageImageUrlParser parser = new PageImageUrlParser(new ImageTagParser(),
                new PixelDensityDescriptorParser());
        PageClient client = PageClientFactory.create(title.getWikiSite(), title.namespace());
        app.getSessionFunnel().leadSectionFetchStart();
        leadReq(client, title).enqueue(new LeadCallback(title.getWikiSite(), parser));
        app.getSessionFunnel().restSectionsFetchStart();
        remainingReq(client, title).enqueue(new RemainingCallback(title.getWikiSite(), parser));
    }

    @NonNull
    private static Call<PageLead> leadReq(@NonNull PageClient client, @NonNull PageTitle title) {
        return client.lead(null, PageClient.CacheOption.CACHE, title.getPrefixedText(),
                calculateLeadImageWidth(), !isImageDownloadEnabled());
    }

    @NonNull
    private static Call<PageRemaining> remainingReq(@NonNull PageClient client,
                                                    @NonNull PageTitle title) {
        return client.sections(null, PageClient.CacheOption.CACHE, title.getPrefixedText(),
                !isImageDownloadEnabled());
    }

    private static void fetchImages(@NonNull final WikiSite wiki,
                                    @NonNull final Iterable<String> urls) {
        OkHttpClient client = OkHttpConnectionFactory.getClient();
        for (String url : urls) {
            url = resolveProtocolRelativeUrl(wiki, url);
            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new ImageCallback());
        }
    }

    private static final class LeadCallback implements Callback<PageLead> {
        @NonNull private PageImageUrlParser parser;
        @NonNull private WikiSite wiki;

        private LeadCallback(@NonNull WikiSite wiki, @NonNull PageImageUrlParser parser) {
            this.parser = parser;
            this.wiki = wiki;
        }

        @Override
        public void onResponse(@NonNull Call<PageLead> call, @NonNull Response<PageLead> response) {
            WikipediaApp.getInstance().getSessionFunnel().leadSectionFetchEnd();
            if (response.body() != null) {
                // noinspection ConstantConditions
                Set<String> imageUrls = new HashSet<>(parser.parse(response.body()));
                fetchImages(wiki, imageUrls);
            }
        }

        @Override
        public void onFailure(@NonNull Call<PageLead> call, @NonNull Throwable t) {
            L.e(t);
        }
    }

    private static final class RemainingCallback implements Callback<PageRemaining> {
        @NonNull private PageImageUrlParser parser;
        @NonNull private WikiSite wiki;

        private RemainingCallback(@NonNull WikiSite wiki, @NonNull PageImageUrlParser parser) {
            this.parser = parser;
            this.wiki = wiki;
        }

        @Override
        public void onResponse(@NonNull Call<PageRemaining> call,
                               @NonNull Response<PageRemaining> response) {
            WikipediaApp.getInstance().getSessionFunnel().restSectionsFetchEnd();
            if (response.body() != null) {
                // noinspection ConstantConditions
                Set<String> imageUrls = new HashSet<>(parser.parse(response.body()));
                fetchImages(wiki, imageUrls);
            }
        }

        @Override
        public void onFailure(@NonNull Call<PageRemaining> call, @NonNull Throwable t) {
            L.e(t);
        }
    }

    private static class ImageCallback implements okhttp3.Callback {
        @Override
        public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
            L.e(e);
        }

        @Override
        public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response)
                throws IOException {
            // Note: raw non-Retrofit usage of OkHttp Requests requires that the Response body is
            // read for the cache to be written.
            if (response.body() != null) {
                // noinspection ConstantConditions
                response.body().close();
            }
        }
    }

    private PageCacher() { }
}

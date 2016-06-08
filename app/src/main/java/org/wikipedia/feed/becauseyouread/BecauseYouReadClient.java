package org.wikipedia.feed.becauseyouread;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.Constants;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;
import org.wikipedia.page.MwApiResultPage;
import org.wikipedia.search.SearchResults;
import org.wikipedia.server.restbase.RbPageEndpointsCache;
import org.wikipedia.zero.WikipediaZeroHandler;

import java.io.IOException;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class BecauseYouReadClient {
    private static final String MORELIKE = "morelike:";

    @NonNull private final MwApiSearchClient client;
    @NonNull private final WikipediaZeroHandler responseHeaderHandler;
    @NonNull private final Retrofit retrofit;
    @NonNull private final Site site;

    public BecauseYouReadClient(@NonNull final Site site) {
        this.site = site;
        this.client = RetrofitFactory.newInstance(site).create(MwApiSearchClient.class);
        this.responseHeaderHandler = WikipediaApp.getInstance().getWikipediaZeroHandler();
        this.retrofit = RbPageEndpointsCache.INSTANCE.getRetrofit();
    }

    public void get(String title, final BecauseYouReadCallback cb) throws IOException {
        Call<MwQueryResponse<Pages>> call = client.get(MORELIKE + title);
        call.enqueue(new Callback<MwQueryResponse<Pages>>() {
            @Override
            public void onResponse(Call<MwQueryResponse<Pages>> call,
                                   Response<MwQueryResponse<Pages>> response) {
                if (response.isSuccessful()) {
                    responseHeaderHandler.onHeaderCheck(response);
                    cb.success(response.body());
                } else {
                    cb.failure(RetrofitException.httpError(response, retrofit));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse<Pages>> call, Throwable t) {
                cb.failure(t);
            }
        });
    }

    public Site site() {
        return site;
    }

    private interface MwApiSearchClient {
        @GET("w/api.php?format=json&formatversion=2&action=query"
                + "&prop=pageterms|pageimages|pageprops&ppprop=mainpage|disambiguation"
                + "&wbptterms=description&generator=search&gsrnamespace=0&gsrwhat=text"
                + "&gsrinfo=&gsrprop=redirecttitle&gsrlimit=" + Constants.SUGGESTION_REQUEST_ITEMS
                + "&piprop=thumbnail&pithumbsize=" + WikipediaApp.PREFERRED_THUMB_SIZE
                + "&pilimit=" + Constants.SUGGESTION_REQUEST_ITEMS)
        @NonNull
        Call<MwQueryResponse<Pages>> get(@Query("gsrsearch") String morelikeQuery);
    }

    public class Pages {
        @SerializedName("pages")
        private MwApiResultPage[] pages;
        public SearchResults results(Site site) {
            return new SearchResults(Arrays.asList(pages), site);
        }
    }

    public interface BecauseYouReadCallback {
        void success(MwQueryResponse<Pages> pages);
        void failure(Throwable throwable);
    }
}

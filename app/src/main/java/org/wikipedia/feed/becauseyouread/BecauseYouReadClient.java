package org.wikipedia.feed.becauseyouread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.Constants;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.model.Card;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.MwApiResultPage;
import org.wikipedia.page.bottomcontent.MainPageReadMoreTopicTask;
import org.wikipedia.search.SearchResults;
import org.wikipedia.util.log.L;
import org.wikipedia.zero.WikipediaZeroHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class BecauseYouReadClient implements FeedClient {
    private static final String MORELIKE = "morelike:";

    @NonNull private final MwCachedService<MwApiSearchClient> cachedService = new MwCachedService<>(MwApiSearchClient.class);
    @NonNull private final WikipediaZeroHandler responseHeaderHandler;

    @Nullable private MainPageReadMoreTopicTask readMoreTopicTask;
    @Nullable private Call<MwQueryResponse<Pages>> readMoreCall;

    public BecauseYouReadClient() {
        this.responseHeaderHandler = WikipediaApp.getInstance().getWikipediaZeroHandler();
    }

    @Override
    public void request(@NonNull Context context, @NonNull final Site site, int age,
                        @NonNull final FeedClient.Callback cb) {
        cancel();
        readMoreTopicTask = new MainPageReadMoreTopicTask(context, age) {
            @Override
            public void onFinish(@Nullable HistoryEntry entry) {
                if (entry == null) {
                    cb.error(new IOException("Error fetching suggestions"));
                    return;
                }
                getSuggestionsForTitle(site, entry, cb);
            }

            @Override
            public void onCatch(Throwable caught) {
                L.w("Error fetching 'because you read' suggestions", caught);
                cb.error(caught);
            }
        };
        readMoreTopicTask.execute();
    }

    @Override
    public void cancel() {
        if (readMoreTopicTask != null) {
            readMoreTopicTask.cancel();
            readMoreTopicTask = null;
        }
        if (readMoreCall != null) {
            readMoreCall.cancel();
            readMoreCall = null;
        }
    }

    private void getSuggestionsForTitle(@NonNull Site site,
                                        @NonNull final HistoryEntry entry,
                                        final FeedClient.Callback cb) {
        final Retrofit retrofit = cachedService.retrofit(site);
        readMoreCall = cachedService.service(site).get(MORELIKE + entry.getTitle().getDisplayText());
        readMoreCall.enqueue(new retrofit2.Callback<MwQueryResponse<Pages>>() {
            @Override
            public void onResponse(Call<MwQueryResponse<Pages>> call,
                                   Response<MwQueryResponse<Pages>> response) {
                if (response.isSuccessful()) {
                    responseHeaderHandler.onHeaderCheck(response);
                    MwQueryResponse<Pages> pages = response.body();
                    if (pages.success() && pages.query() != null && pages.query().results(entry.getTitle().getSite()) != null) {
                        SearchResults results = SearchResults.filter(pages.query().results(entry.getTitle().getSite()), entry.getTitle().getText(), false);
                        List<BecauseYouReadItemCard> itemCards = MwApiResultPage.searchResultsToCards(results, entry.getTitle().getSite());

                        cb.success(Collections.singletonList((Card) new BecauseYouReadCard(entry, itemCards)));
                    } else {
                        cb.error(new IOException("Error fetching suggestions."));
                    }
                } else {
                    cb.error(RetrofitException.httpError(response, retrofit));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse<Pages>> call, Throwable t) {
                cb.error(t);
            }
        });
    }

    private interface MwApiSearchClient {
        @GET("w/api.php?format=json&formatversion=2&action=query"
                + "&prop=pageterms|pageimages|pageprops&ppprop=mainpage|disambiguation"
                + "&wbptterms=description&generator=search&gsrnamespace=0&gsrwhat=text"
                + "&gsrinfo=&gsrprop=redirecttitle&gsrlimit=" + Constants.SUGGESTION_REQUEST_ITEMS
                + "&piprop=thumbnail&pithumbsize=" + Constants.PREFERRED_THUMB_SIZE
                + "&pilimit=" + Constants.SUGGESTION_REQUEST_ITEMS)
        @NonNull
        Call<MwQueryResponse<Pages>> get(@Query("gsrsearch") String morelikeQuery);
    }

    public class Pages {
        @SuppressWarnings("unused")
        @SerializedName("pages")
        private MwApiResultPage[] pages;
        public SearchResults results(Site site) {
            return new SearchResults(Arrays.asList(pages), site);
        }
    }
}

package org.wikipedia.feed.becauseyouread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.Constants;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.dataclient.retrofit.WikiCachedService;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.model.Card;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.bottomcontent.MainPageReadMoreTopicTask;
import org.wikipedia.search.SearchResult;
import org.wikipedia.search.SearchResults;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class BecauseYouReadClient implements FeedClient {
    private static final String MORELIKE = "morelike:";

    @NonNull private final WikiCachedService<MwApiSearchClient> cachedService = new MwCachedService<>(MwApiSearchClient.class);

    @Nullable private MainPageReadMoreTopicTask readMoreTopicTask;
    @Nullable private Call<MwQueryResponse<Pages>> readMoreCall;

    @Override public void request(@NonNull Context context, @NonNull final WikiSite wiki, int age,
                        @NonNull final FeedClient.Callback cb) {
        cancel();
        readMoreTopicTask = new MainPageReadMoreTopicTask(context, age) {
            @Override
            public void onFinish(@Nullable HistoryEntry entry) {
                if (entry == null) {
                    cb.error(new IOException("Error fetching suggestions"));
                    return;
                }
                getSuggestionsForTitle(wiki, entry, cb);
            }

            @Override
            public void onCatch(Throwable caught) {
                L.w("Error fetching 'because you read' suggestions", caught);
                cb.error(caught);
            }
        };
        readMoreTopicTask.execute();
    }

    @Override public void cancel() {
        if (readMoreTopicTask != null) {
            readMoreTopicTask.cancel();
            readMoreTopicTask = null;
        }
        if (readMoreCall != null) {
            readMoreCall.cancel();
            readMoreCall = null;
        }
    }

    private void getSuggestionsForTitle(@NonNull WikiSite wiki,
                                        @NonNull final HistoryEntry entry,
                                        final FeedClient.Callback cb) {
        readMoreCall = cachedService.service(wiki).get(MORELIKE + entry.getTitle().getDisplayText());
        readMoreCall.enqueue(new retrofit2.Callback<MwQueryResponse<Pages>>() {
            @Override
            public void onResponse(Call<MwQueryResponse<Pages>> call,
                                   Response<MwQueryResponse<Pages>> response) {
                if (response.isSuccessful()) {
                    MwQueryResponse<Pages> pages = response.body();
                    if (pages.success() && pages.query().results(entry.getTitle().getWikiSite()) != null) {
                        SearchResults results = SearchResults.filter(pages.query().results(entry.getTitle().getWikiSite()), entry.getTitle().getText(), false);
                        List<BecauseYouReadItemCard> itemCards = new ArrayList<>();
                        for (SearchResult result : results.getResults()) {
                            itemCards.add(new BecauseYouReadItemCard(result.getPageTitle()));
                        }
                        cb.success(Collections.singletonList((Card) new BecauseYouReadCard(entry, itemCards)));
                    } else if (pages.hasError()) {
                        cb.error(new MwException(pages.getError()));
                    } else {
                        cb.error(new IOException("Error fetching suggestions."));
                    }
                } else {
                    cb.error(RetrofitException.httpError(response));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse<Pages>> call, Throwable t) {
                cb.error(t);
            }
        });
    }

    static class Pages {
        @SuppressWarnings("unused")
        @SerializedName("pages")
        private List<MwQueryPage> pages;
        public SearchResults results(WikiSite wiki) {
            return new SearchResults(pages, wiki);
        }
    }

    private interface MwApiSearchClient {
        @GET("w/api.php?format=json&formatversion=2&action=query"
                + "&prop=pageterms|pageimages|pageprops&ppprop=mainpage|disambiguation"
                + "&wbptterms=description&generator=search&gsrnamespace=0&gsrwhat=text"
                + "&gsrinfo=&gsrprop=redirecttitle&gsrlimit=" + Constants.SUGGESTION_REQUEST_ITEMS
                + "&piprop=thumbnail&pilicense=any&pithumbsize=" + Constants.PREFERRED_THUMB_SIZE
                + "&pilimit=" + Constants.SUGGESTION_REQUEST_ITEMS)
        @NonNull Call<MwQueryResponse<Pages>> get(@Query("gsrsearch") String morelikeQuery);
    }
}

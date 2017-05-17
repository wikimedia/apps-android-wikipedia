package org.wikipedia.feed.aggregated;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.featured.FeaturedArticleCard;
import org.wikipedia.feed.image.FeaturedImageCard;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.UtcDate;
import org.wikipedia.feed.mostread.MostReadListCard;
import org.wikipedia.feed.news.NewsListCard;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

import static org.wikipedia.Constants.ACCEPT_HEADER_PREFIX;

public class AggregatedFeedContentClient implements FeedClient {

    @Nullable private Call<AggregatedFeedContent> call;

    @Override
    public void request(@NonNull Context context, @NonNull WikiSite wiki, int age, @NonNull Callback cb) {
        cancel();
        UtcDate date = DateUtil.getUtcRequestDateFor(age);
        String endpoint = String.format(Locale.ROOT, Prefs.getRestbaseUriFormat(), wiki.scheme(),
                wiki.authority());
        Retrofit retrofit = RetrofitFactory.newInstance(endpoint, wiki);
        AggregatedFeedContentClient.Service service = retrofit.create(Service.class);
        call = service.get(date.year(), date.month(), date.date());
        call.enqueue(new CallbackAdapter(cb, wiki, age));
    }

    @Override
    public void cancel() {
        if (call == null) {
            return;
        }
        call.cancel();
        call = null;
    }

    private interface Service {

        /**
         * Gets aggregated content for the feed for the date provided.
         *
         * @param year four-digit year
         * @param month two-digit month
         * @param day two-digit day
         */
        @NonNull
        @Headers(ACCEPT_HEADER_PREFIX + "aggregated-feed/0.5.0\"")
        @GET("feed/featured/{year}/{month}/{day}")
        Call<AggregatedFeedContent> get(@Path("year") String year,
                                        @Path("month") String month,
                                        @Path("day") String day);
    }

    private static class CallbackAdapter implements retrofit2.Callback<AggregatedFeedContent> {
        @NonNull private final Callback cb;
        @NonNull private final WikiSite wiki;
        private final int age;

        CallbackAdapter(@NonNull Callback cb, @NonNull WikiSite wiki, int age) {
            this.cb = cb;
            this.wiki = wiki;
            this.age = age;
        }

        @Override public void onResponse(Call<AggregatedFeedContent> call,
                                         Response<AggregatedFeedContent> response) {
            List<Card> cards = new ArrayList<>();
            AggregatedFeedContent content = response.body();
            // todo: remove age check when news endpoint provides dated content, T139481.
            if (age == 0 && content.news() != null) {
                cards.add(new NewsListCard(content.news(), age, wiki));
            }
            if (content.tfa() != null) {
                cards.add(new FeaturedArticleCard(content.tfa(), age, wiki));
            }
            if (content.mostRead() != null) {
                cards.add(new MostReadListCard(content.mostRead(), wiki));
            }
            if (content.potd() != null) {
                cards.add(new FeaturedImageCard(content.potd(), age, wiki));
            }
            cb.success(cards);
        }

        @Override public void onFailure(Call<AggregatedFeedContent> call, Throwable caught) {
            L.v(caught);
            cb.error(caught);
        }
    }
}

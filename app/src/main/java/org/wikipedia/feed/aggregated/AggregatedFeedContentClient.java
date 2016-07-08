package org.wikipedia.feed.aggregated;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.Site;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;
import org.wikipedia.feed.FeedClient;
import org.wikipedia.feed.UtcDate;
import org.wikipedia.feed.featured.FeaturedArticleCard;
import org.wikipedia.feed.image.FeaturedImageCard;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.mostread.MostReadListCard;
import org.wikipedia.feed.news.NewsListCard;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class AggregatedFeedContentClient implements FeedClient {

    @Nullable private Call<AggregatedFeedContent> call;
    @Nullable private static UtcDate DATE;

    @Override
    public void request(@NonNull Context context, @NonNull Site site, int age, @NonNull Callback cb) {
        cancel();
        DATE = DateUtil.getUtcRequestDateFor(age);
        // TODO: Use app retrofit, etc., when feed endpoints are deployed to production
        Retrofit retrofit = RetrofitFactory.newInstance(site,
                String.format(Locale.ROOT, Prefs.getRestbaseUriFormat(), "http", site.authority()));
        AggregatedFeedContentClient.Service service = retrofit.create(Service.class);
        call = service.get(DATE.year(), DATE.month(), DATE.date());
        call.enqueue(new CallbackAdapter(cb, site, age));
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
        @GET("feed/featured/{year}/{month}/{day}")
        Call<AggregatedFeedContent> get(@Path("year") String year,
                                        @Path("month") String month,
                                        @Path("day") String day);
    }

    private static class CallbackAdapter implements retrofit2.Callback<AggregatedFeedContent> {
        @NonNull private final Callback cb;
        @NonNull private final Site site;
        private final int age;

        CallbackAdapter(@NonNull Callback cb, @NonNull Site site, int age) {
            this.cb = cb;
            this.site = site;
            this.age = age;
        }

        @Override public void onResponse(Call<AggregatedFeedContent> call,
                                         Response<AggregatedFeedContent> response) {
            if (response.isSuccessful()) {
                List<Card> cards = new ArrayList<>();
                AggregatedFeedContent content = response.body();
                if (content.tfa() != null) {
                    cards.add(new FeaturedArticleCard(content.tfa(), DATE, site));
                }
                // todo: remove age check when news endpoint provides dated content, T139481.
                if (age == 0 && content.news() != null) {
                    cards.add(new NewsListCard(content.news(), DATE, site));
                }
                if (content.mostRead() != null) {
                    cards.add(new MostReadListCard(content.mostRead(), site));
                }
                if (content.potd() != null) {
                    cards.add(new FeaturedImageCard(content.potd(), DATE, site));
                }
                cb.success(cards);
            } else {
                L.v(response.message());
                cb.error(new IOException(response.message()));
            }
        }

        @Override public void onFailure(Call<AggregatedFeedContent> call, Throwable caught) {
            L.v(caught);
            cb.error(caught);
        }
    }
}

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
import org.wikipedia.feed.onthisday.OnThisDayCard;
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

public class AggregatedFeedContentClient {
    @Nullable private Call<AggregatedFeedContent> call;
    @Nullable private AggregatedFeedContent aggregatedResponse;
    private int aggregatedResponseAge = -1;

    public static class OnThisDayFeed extends BaseClient {
        public OnThisDayFeed(@NonNull AggregatedFeedContentClient aggregatedClient) {
            super(aggregatedClient);
        }

        @Override
        void getCardFromResponse(@NonNull AggregatedFeedContent content, @NonNull WikiSite wiki,
                                 int age, @NonNull List<Card> outCards) {
            if (content.onthisday() != null && !content.onthisday().isEmpty()) {
                outCards.add(new OnThisDayCard(content.onthisday(), wiki, age));
            }
        }
    }
    public static class InTheNews extends BaseClient {
        public InTheNews(@NonNull AggregatedFeedContentClient aggregatedClient) {
            super(aggregatedClient);
        }

        @Override
        void getCardFromResponse(@NonNull AggregatedFeedContent content, @NonNull WikiSite wiki,
                                 int age, @NonNull List<Card> outCards) {
            // todo: remove age check when news endpoint provides dated content, T139481.
            if (age == 0 && content.news() != null) {
                outCards.add(new NewsListCard(content.news(), age, wiki));
            }
        }
    }

    public static class FeaturedArticle extends BaseClient {
        public FeaturedArticle(@NonNull AggregatedFeedContentClient aggregatedClient) {
            super(aggregatedClient);
        }

        @Override
        void getCardFromResponse(@NonNull AggregatedFeedContent content, @NonNull WikiSite wiki,
                                 int age, @NonNull List<Card> outCards) {
            if (content.tfa() != null) {
                outCards.add(new FeaturedArticleCard(content.tfa(), age, wiki));
            }
        }
    }

    public static class TrendingArticles extends BaseClient {
        public TrendingArticles(@NonNull AggregatedFeedContentClient aggregatedClient) {
            super(aggregatedClient);
        }

        @Override
        void getCardFromResponse(@NonNull AggregatedFeedContent content, @NonNull WikiSite wiki,
                                 int age, @NonNull List<Card> outCards) {
            if (content.mostRead() != null) {
                outCards.add(new MostReadListCard(content.mostRead(), wiki));
            }
        }
    }

    public static class FeaturedImage extends BaseClient {
        public FeaturedImage(@NonNull AggregatedFeedContentClient aggregatedClient) {
            super(aggregatedClient);
        }

        @Override
        void getCardFromResponse(@NonNull AggregatedFeedContent content, @NonNull WikiSite wiki,
                                 int age, @NonNull List<Card> outCards) {
            if (content.potd() != null) {
                outCards.add(new FeaturedImageCard(content.potd(), age, wiki));
            }
        }
    }

    void setAggregatedResponse(@Nullable AggregatedFeedContent content, int age) {
        aggregatedResponse = content;
        this.aggregatedResponseAge = age;
    }

    @Nullable AggregatedFeedContent getCurrentResponse() {
        return aggregatedResponse;
    }

    int getCurrentAge() {
        return aggregatedResponseAge;
    }

    void requestAggregated(@NonNull WikiSite wiki, int age, @NonNull retrofit2.Callback<AggregatedFeedContent> cb) {
        cancel();
        UtcDate date = DateUtil.getUtcRequestDateFor(age);
        String endpoint = String.format(Locale.ROOT, Prefs.getRestbaseUriFormat(), wiki.scheme(),
                wiki.authority());
        Retrofit retrofit = RetrofitFactory.newInstance(endpoint, wiki);
        AggregatedFeedContentClient.Service service = retrofit.create(Service.class);
        call = service.get(date.year(), date.month(), date.date());
        call.enqueue(cb);
    }

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

    private abstract static class BaseClient implements FeedClient, retrofit2.Callback<AggregatedFeedContent> {
        @NonNull private AggregatedFeedContentClient aggregatedClient;
        @Nullable private Callback cb;
        private WikiSite wiki;
        private int age;

        BaseClient(@NonNull AggregatedFeedContentClient aggregatedClient) {
            this.aggregatedClient = aggregatedClient;
        }

        abstract void getCardFromResponse(@NonNull AggregatedFeedContent response,
                                          @NonNull WikiSite wiki, int age, @NonNull List<Card> outCards);

        @Override
        public void request(@NonNull Context context, @NonNull WikiSite wiki, int age, @NonNull Callback cb) {
            this.cb = cb;
            this.age = age;
            if (aggregatedClient.getCurrentAge() == age
                    && aggregatedClient.getCurrentResponse() != null
                    && wiki.equals(this.wiki)) {
                List<Card> cards = new ArrayList<>();
                getCardFromResponse(aggregatedClient.getCurrentResponse(), wiki, age, cards);
                cb.success(cards);
            } else {
                aggregatedClient.requestAggregated(wiki, age, this);
            }
            this.wiki = wiki;
        }

        @Override
        public void cancel() {
        }

        @Override public void onResponse(@NonNull Call<AggregatedFeedContent> call,
                                         @NonNull Response<AggregatedFeedContent> response) {
            AggregatedFeedContent content = response.body();
            if (content == null) {
                if (cb != null) {
                    cb.error(new RuntimeException("Aggregated response was not in the correct format."));
                }
                return;
            }
            aggregatedClient.setAggregatedResponse(content, age);
            List<Card> cards = new ArrayList<>();
            if (aggregatedClient.getCurrentResponse() != null) {
                getCardFromResponse(aggregatedClient.getCurrentResponse(), wiki, age, cards);
            }
            if (cb != null) {
                cb.success(cards);
            }
        }

        @Override public void onFailure(@NonNull Call<AggregatedFeedContent> call, @NonNull Throwable caught) {
            L.v(caught);
            if (cb != null) {
                cb.error(caught);
            }
        }
    }
}

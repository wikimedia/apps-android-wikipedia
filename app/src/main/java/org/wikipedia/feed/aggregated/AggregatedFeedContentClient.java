package org.wikipedia.feed.aggregated;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.FeedContentType;
import org.wikipedia.feed.FeedCoordinator;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.featured.FeaturedArticleCard;
import org.wikipedia.feed.image.FeaturedImageCard;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.UtcDate;
import org.wikipedia.feed.mostread.MostReadListCard;
import org.wikipedia.feed.news.NewsCard;
import org.wikipedia.feed.onthisday.OnThisDayCard;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AggregatedFeedContentClient {
    @NonNull private Map<String, AggregatedFeedContent> aggregatedResponses = new HashMap<>();
    private int aggregatedResponseAge = -1;
    private CompositeDisposable disposables = new CompositeDisposable();

    public static class OnThisDayFeed extends BaseClient {
        public OnThisDayFeed(@NonNull AggregatedFeedContentClient aggregatedClient) {
            super(aggregatedClient);
        }

        @Override
        void getCardFromResponse(@NonNull Map<String, AggregatedFeedContent> responses,
                                 @NonNull WikiSite wiki, int age, @NonNull List<Card> outCards) {
            for (String appLangCode : WikipediaApp.getInstance().language().getAppLanguageCodes()) {
                if (responses.containsKey(appLangCode)
                        && !FeedContentType.ON_THIS_DAY.getLangCodesDisabled().contains(appLangCode)) {
                    AggregatedFeedContent content = responses.get(appLangCode);
                    if (content.onthisday() != null && !content.onthisday().isEmpty()) {
                        outCards.add(new OnThisDayCard(content.onthisday(), WikiSite.forLanguageCode(appLangCode), age));
                    }
                }
            }
        }
    }
    public static class InTheNews extends BaseClient {
        public InTheNews(@NonNull AggregatedFeedContentClient aggregatedClient) {
            super(aggregatedClient);
        }

        @Override
        void getCardFromResponse(@NonNull Map<String, AggregatedFeedContent> responses,
                                 @NonNull WikiSite wiki, int age, @NonNull List<Card> outCards) {
            // todo: remove age check when news endpoint provides dated content, T139481.
            for (String appLangCode : WikipediaApp.getInstance().language().getAppLanguageCodes()) {
                if (responses.containsKey(appLangCode)
                        && !FeedContentType.NEWS.getLangCodesDisabled().contains(appLangCode)) {
                    AggregatedFeedContent content = responses.get(appLangCode);
                    if (age == 0 && content.news() != null) {
                        outCards.add(new NewsCard(content.news(), age, WikiSite.forLanguageCode(appLangCode)));
                    }
                }
            }
        }
    }

    public static class FeaturedArticle extends BaseClient {
        public FeaturedArticle(@NonNull AggregatedFeedContentClient aggregatedClient) {
            super(aggregatedClient);
        }

        @Override
        void getCardFromResponse(@NonNull Map<String, AggregatedFeedContent> responses,
                                 @NonNull WikiSite wiki, int age, @NonNull List<Card> outCards) {
            for (String appLangCode : WikipediaApp.getInstance().language().getAppLanguageCodes()) {
                if (responses.containsKey(appLangCode)
                        && !FeedContentType.FEATURED_ARTICLE.getLangCodesDisabled().contains(appLangCode)) {
                    AggregatedFeedContent content = responses.get(appLangCode);
                    if (content.tfa() != null) {
                        outCards.add(new FeaturedArticleCard(content.tfa(), age, WikiSite.forLanguageCode(appLangCode)));
                    }
                }
            }
        }
    }

    public static class TrendingArticles extends BaseClient {
        public TrendingArticles(@NonNull AggregatedFeedContentClient aggregatedClient) {
            super(aggregatedClient);
        }

        @Override
        void getCardFromResponse(@NonNull Map<String, AggregatedFeedContent> responses,
                                 @NonNull WikiSite wiki, int age, @NonNull List<Card> outCards) {
            for (String appLangCode : WikipediaApp.getInstance().language().getAppLanguageCodes()) {
                if (responses.containsKey(appLangCode)
                        && !FeedContentType.TRENDING_ARTICLES.getLangCodesDisabled().contains(appLangCode)) {
                    AggregatedFeedContent content = responses.get(appLangCode);
                    if (content.mostRead() != null) {
                        outCards.add(new MostReadListCard(content.mostRead(), WikiSite.forLanguageCode(appLangCode)));
                    }
                }
            }
        }
    }

    public static class FeaturedImage extends BaseClient {
        public FeaturedImage(@NonNull AggregatedFeedContentClient aggregatedClient) {
            super(aggregatedClient);
        }

        @Override
        void getCardFromResponse(@NonNull Map<String, AggregatedFeedContent> responses,
                                 @NonNull WikiSite wiki, int age, @NonNull List<Card> outCards) {
            if (responses.containsKey(wiki.languageCode())) {
                AggregatedFeedContent content = responses.get(wiki.languageCode());
                if (content.potd() != null) {
                    outCards.add(new FeaturedImageCard(content.potd(), age, wiki));
                }
            }
        }
    }

    public void invalidate() {
        aggregatedResponseAge = -1;
    }

    public void cancel() {
        disposables.clear();
    }

    private abstract static class BaseClient implements FeedClient {
        @NonNull private AggregatedFeedContentClient aggregatedClient;
        @Nullable private Callback cb;
        private WikiSite wiki;
        private int age;

        BaseClient(@NonNull AggregatedFeedContentClient aggregatedClient) {
            this.aggregatedClient = aggregatedClient;
        }

        abstract void getCardFromResponse(@NonNull Map<String, AggregatedFeedContent> responses,
                                          @NonNull WikiSite wiki, int age, @NonNull List<Card> outCards);

        @Override
        public void request(@NonNull Context context, @NonNull WikiSite wiki, int age, @NonNull Callback cb) {
            this.cb = cb;
            this.age = age;
            this.wiki = wiki;
            if (aggregatedClient.aggregatedResponseAge == age
                    && aggregatedClient.aggregatedResponses.containsKey(wiki.languageCode())) {
                List<Card> cards = new ArrayList<>();
                getCardFromResponse(aggregatedClient.aggregatedResponses, wiki, age, cards);
                FeedCoordinator.postCardsToCallback(cb, cards);
            } else {
                requestAggregated();
            }
        }

        @Override
        public void cancel() {
        }

        private void requestAggregated() {
            aggregatedClient.cancel();
            UtcDate date = DateUtil.getUtcRequestDateFor(age);
            aggregatedClient.disposables.add(Observable.fromIterable(FeedContentType.getAggregatedLanguages())
                    .flatMap(lang -> ServiceFactory.getRest(WikiSite.forLanguageCode(lang)).getAggregatedFeed(date.getYear(), date.getMonth(), date.getDay()).subscribeOn(Schedulers.io()), Pair::new)
                    .toList()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(pairList -> {
                        List<Card> cards = new ArrayList<>();
                        for (Pair<String, AggregatedFeedContent> pair : pairList) {
                            AggregatedFeedContent content = pair.second;
                            if (content == null) {
                                continue;
                            }
                            aggregatedClient.aggregatedResponses.put(WikiSite.forLanguageCode(pair.first).languageCode(), content);
                            aggregatedClient.aggregatedResponseAge = age;
                        }
                        if (aggregatedClient.aggregatedResponses.containsKey(wiki.languageCode())) {
                            getCardFromResponse(aggregatedClient.aggregatedResponses, wiki, age, cards);
                        }
                        if (cb != null) {
                            FeedCoordinator.postCardsToCallback(cb, cards);
                        }
                    }, caught -> {
                        L.v(caught);
                        if (cb != null) {
                            cb.error(caught);
                        }
                    }));
        }
    }
}

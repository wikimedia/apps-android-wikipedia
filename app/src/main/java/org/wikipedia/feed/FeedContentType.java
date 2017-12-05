package org.wikipedia.feed;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import org.wikipedia.R;
import org.wikipedia.feed.aggregated.AggregatedFeedContentClient;
import org.wikipedia.feed.becauseyouread.BecauseYouReadClient;
import org.wikipedia.feed.continuereading.ContinueReadingClient;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.mainpage.MainPageClient;
import org.wikipedia.feed.random.RandomClient;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;
import org.wikipedia.settings.Prefs;

import java.util.ArrayList;
import java.util.List;

public enum FeedContentType implements EnumCode {
    NEWS(0, R.string.view_card_news_title, R.string.feed_item_type_news) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age, boolean isOnline) {
            return isEnabled() && age == 0 && isOnline ? new AggregatedFeedContentClient.InTheNews(aggregatedClient) : null;
        }
    },
    ON_THIS_DAY(1, R.string.on_this_day_card_title, R.string.feed_item_type_on_this_day) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age, boolean isOnline) {
            return isEnabled() && isOnline ? new AggregatedFeedContentClient.OnThisDayFeed(aggregatedClient) : null;
        }
    },
    CONTINUE_READING(2, R.string.view_continue_reading_card_title, R.string.feed_item_type_continue_reading) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age, boolean isOnline) {
            return isEnabled() ? new ContinueReadingClient() : null;
        }
    },
    TRENDING_ARTICLES(3, R.string.most_read_list_card_title, R.string.feed_item_type_trending) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age, boolean isOnline) {
            return isEnabled() && isOnline ? new AggregatedFeedContentClient.TrendingArticles(aggregatedClient) : null;
        }
    },
    MAIN_PAGE(4, R.string.view_main_page_card_title, R.string.feed_item_type_main_page) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age, boolean isOnline) {
            return isEnabled() && age == 0 ? new MainPageClient() : null;
        }
    },
    RANDOM(5, R.string.view_random_card_title, R.string.feed_item_type_randomizer) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age, boolean isOnline) {
            return isEnabled() && age % 2 == 0 ? new RandomClient() : null;
        }
    },
    FEATURED_ARTICLE(6, R.string.view_featured_article_card_title, R.string.feed_item_type_featured_article) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age, boolean isOnline) {
            return isEnabled() && isOnline ? new AggregatedFeedContentClient.FeaturedArticle(aggregatedClient) : null;
        }
    },
    FEATURED_IMAGE(7, R.string.view_featured_image_card_title, R.string.feed_item_type_featured_image) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age, boolean isOnline) {
            return isEnabled() && isOnline ? new AggregatedFeedContentClient.FeaturedImage(aggregatedClient) : null;
        }
    },
    BECAUSE_YOU_READ(8, R.string.view_because_you_read_card_title, R.string.feed_item_type_because_you_read) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age, boolean isOnline) {
            return isEnabled() && isOnline ? new BecauseYouReadClient() : null;
        }
    };

    private static final EnumCodeMap<FeedContentType> MAP
            = new EnumCodeMap<>(FeedContentType.class);
    private final int code;
    @StringRes private final int titleId;
    @StringRes private final int subtitleId;
    private int order;
    private boolean enabled = true;

    @NonNull public static FeedContentType of(int code) {
        return MAP.get(code);
    }

    @Nullable
    public abstract FeedClient newClient(AggregatedFeedContentClient aggregatedClient,
                                         int age, boolean isOnline);

    @Override public int code() {
        return code;
    }

    @StringRes public int titleId() {
        return titleId;
    }

    @StringRes public int subtitleId() {
        return subtitleId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    FeedContentType(int code, @StringRes int titleId, @StringRes int subtitleId) {
        this.code = code;
        this.order = code;
        this.titleId = titleId;
        this.subtitleId = subtitleId;
    }

    public static void saveState() {
        List<Boolean> enabledList = new ArrayList<>();
        List<Integer> orderList = new ArrayList<>();
        for (int i = 0; i < FeedContentType.values().length; i++) {
            enabledList.add(FeedContentType.values()[i].isEnabled());
            orderList.add(FeedContentType.values()[i].getOrder());
        }
        Prefs.setFeedCardsEnabled(enabledList);
        Prefs.setFeedCardsOrder(orderList);
    }

    public static void restoreState() {
        List<Boolean> enabledList = Prefs.getFeedCardsEnabled();
        List<Integer> orderList = Prefs.getFeedCardsOrder();
        for (int i = 0; i < FeedContentType.values().length; i++) {
            FeedContentType.values()[i].setEnabled(i < enabledList.size() ? enabledList.get(i) : true);
            FeedContentType.values()[i].setOrder(i < orderList.size() ? orderList.get(i) : i);
        }
    }
}

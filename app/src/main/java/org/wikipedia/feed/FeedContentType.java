package org.wikipedia.feed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.feed.accessibility.AccessibilityCardClient;
import org.wikipedia.feed.aggregated.AggregatedFeedContentClient;
import org.wikipedia.feed.becauseyouread.BecauseYouReadClient;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.mainpage.MainPageClient;
import org.wikipedia.feed.random.RandomClient;
import org.wikipedia.feed.suggestededits.SuggestedEditsFeedClient;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DeviceUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum FeedContentType implements EnumCode {
    FEATURED_ARTICLE(6, R.string.view_featured_article_card_title, R.string.feed_item_type_featured_article, true) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age) {
            return isEnabled() ? new AggregatedFeedContentClient.FeaturedArticle(aggregatedClient) : null;
        }
    },
    TRENDING_ARTICLES(3, R.string.view_top_read_card_title, R.string.feed_item_type_trending, true) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age) {
            return isEnabled() ? new AggregatedFeedContentClient.TrendingArticles(aggregatedClient) : null;
        }
    },
    FEATURED_IMAGE(7, R.string.view_featured_image_card_title, R.string.feed_item_type_featured_image, false) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age) {
            return isEnabled() ? new AggregatedFeedContentClient.FeaturedImage(aggregatedClient) : null;
        }
    },
    BECAUSE_YOU_READ(8, R.string.view_because_you_read_card_title, R.string.feed_item_type_because_you_read, false) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age) {
            return isEnabled() ? new BecauseYouReadClient() : null;
        }
    },
    NEWS(0, R.string.view_card_news_title, R.string.feed_item_type_news, true) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age) {
            return isEnabled() && age == 0 ? new AggregatedFeedContentClient.InTheNews(aggregatedClient) : null;
        }
    },
    ON_THIS_DAY(1, R.string.on_this_day_card_title, R.string.feed_item_type_on_this_day, true) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age) {
            return isEnabled() ? new AggregatedFeedContentClient.OnThisDayFeed(aggregatedClient) : null;
        }
    },
    RANDOM(5, R.string.view_random_card_title, R.string.feed_item_type_randomizer, true) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age) {
            return isEnabled() ? new RandomClient() : null;
        }
    },
    MAIN_PAGE(4, R.string.view_main_page_card_title, R.string.feed_item_type_main_page, true) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age) {
            return isEnabled() && age == 0 ? new MainPageClient() : null;
        }
    },
    SUGGESTED_EDITS(9, R.string.suggested_edits_feed_card_title, R.string.feed_item_type_suggested_edits, false) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age) {
            if (isEnabled() && AccountUtil.isLoggedIn() && WikipediaApp.getInstance().isOnline()) {
                return new SuggestedEditsFeedClient();
            }
            return null;
        }
    },
    ACCESSIBILITY(10, 0, 0, false, false) {
        @Nullable
        @Override
        public FeedClient newClient(AggregatedFeedContentClient aggregatedClient, int age) {
            return DeviceUtil.isAccessibilityEnabled() ? new AccessibilityCardClient() : null;
        }
    };

    private static final EnumCodeMap<FeedContentType> MAP
            = new EnumCodeMap<>(FeedContentType.class);
    private final int code;
    @StringRes private final int titleId;
    @StringRes private final int subtitleId;
    private int order;
    private boolean enabled = true;

    private boolean perLanguage;
    private boolean showInConfig = true;
    private List<String> langCodesSupported = new ArrayList<>();
    private List<String> langCodesDisabled = new ArrayList<>();

    @NonNull public static FeedContentType of(int code) {
        return MAP.get(code);
    }

    @Nullable
    public abstract FeedClient newClient(AggregatedFeedContentClient aggregatedClient,
                                         int age);

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

    public boolean showInConfig() {
        return showInConfig;
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

    public boolean isPerLanguage() {
        return perLanguage;
    }

    public List<String> getLangCodesSupported() {
        return langCodesSupported;
    }

    public List<String> getLangCodesDisabled() {
        return langCodesDisabled;
    }

    FeedContentType(int code, @StringRes int titleId, @StringRes int subtitleId, boolean perLanguage) {
        this.code = code;
        this.order = code;
        this.titleId = titleId;
        this.subtitleId = subtitleId;
        this.perLanguage = perLanguage;
    }

    FeedContentType(int code, @StringRes int titleId, @StringRes int subtitleId, boolean perLanguage, boolean showInConfig) {
        this(code, titleId, subtitleId, perLanguage);
        this.showInConfig = showInConfig;
    }

    public static List<String> getAggregatedLanguages() {
        List<String> appLangCodes = WikipediaApp.getInstance().language().getAppLanguageCodes();
        List<String> list = new ArrayList<>();
        for (int i = 0; i < FeedContentType.values().length; i++) {
            FeedContentType type = FeedContentType.values()[i];
            if (!type.isEnabled()) {
                continue;
            }
            for (String appLangCode : appLangCodes) {
                if ((type.getLangCodesSupported().isEmpty() || type.getLangCodesSupported().contains(appLangCode))
                        && !type.getLangCodesDisabled().contains(appLangCode)
                        && !list.contains(appLangCode)) {
                    list.add(appLangCode);
                }
            }
        }
        return list;
    }

    public static void saveState() {
        List<Boolean> enabledList = new ArrayList<>();
        List<Integer> orderList = new ArrayList<>();
        Map<Integer, List<String>> langSupportedMap = new HashMap<>();
        Map<Integer, List<String>> langDisabledMap = new HashMap<>();

        for (int i = 0; i < FeedContentType.values().length; i++) {
            FeedContentType type = FeedContentType.values()[i];
            enabledList.add(type.isEnabled());
            orderList.add(type.getOrder());

            langSupportedMap.put(type.code, type.langCodesSupported);
            langDisabledMap.put(type.code, type.langCodesDisabled);
        }
        Prefs.setFeedCardsEnabled(enabledList);
        Prefs.setFeedCardsOrder(orderList);
        Prefs.setFeedCardsLangSupported(langSupportedMap);
        Prefs.setFeedCardsLangDisabled(langDisabledMap);
    }

    public static void restoreState() {
        List<Boolean> enabledList = Prefs.getFeedCardsEnabled();
        List<Integer> orderList = Prefs.getFeedCardsOrder();
        Map<Integer, List<String>> langSupportedMap = Prefs.getFeedCardsLangSupported();
        Map<Integer, List<String>> langDisabledMap = Prefs.getFeedCardsLangDisabled();
        for (int i = 0; i < FeedContentType.values().length; i++) {
            FeedContentType type = FeedContentType.values()[i];
            type.setEnabled(i < enabledList.size() ? enabledList.get(i) : true);
            type.setOrder(i < orderList.size() ? orderList.get(i) : i);
            type.langCodesSupported.clear();
            type.langCodesSupported.addAll(langSupportedMap.getOrDefault(type.code, Collections.emptyList()));
            type.langCodesDisabled.clear();
            type.langCodesDisabled.addAll(langDisabledMap.getOrDefault(type.code, Collections.emptyList()));
        }
    }
}

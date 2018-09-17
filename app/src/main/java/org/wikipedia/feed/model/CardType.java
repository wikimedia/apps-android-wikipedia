package org.wikipedia.feed.model;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.feed.FeedContentType;
import org.wikipedia.feed.announcement.AnnouncementCardView;
import org.wikipedia.feed.becauseyouread.BecauseYouReadCardView;
import org.wikipedia.feed.dayheader.DayHeaderCardView;
import org.wikipedia.feed.featured.FeaturedArticleCardView;
import org.wikipedia.feed.image.FeaturedImageCardView;
import org.wikipedia.feed.mainpage.MainPageCardView;
import org.wikipedia.feed.mostread.MostReadCardView;
import org.wikipedia.feed.news.NewsListCardView;
import org.wikipedia.feed.offline.OfflineCardView;
import org.wikipedia.feed.onthisday.OnThisDayCardView;
import org.wikipedia.feed.progress.ProgressCardView;
import org.wikipedia.feed.random.RandomCardView;
import org.wikipedia.feed.searchbar.SearchCardView;
import org.wikipedia.feed.view.FeedCardView;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;

public enum CardType implements EnumCode {
    SEARCH_BAR(0) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new SearchCardView(ctx);
        }
    },
    BECAUSE_YOU_READ_LIST(2, FeedContentType.BECAUSE_YOU_READ) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new BecauseYouReadCardView(ctx);
        }
    },
    MOST_READ_LIST(3, FeedContentType.TRENDING_ARTICLES) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new MostReadCardView(ctx);
        }
    },
    FEATURED_ARTICLE(4, FeedContentType.FEATURED_ARTICLE) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new FeaturedArticleCardView(ctx);
        }
    },
    RANDOM(5, FeedContentType.RANDOM) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new RandomCardView(ctx);
        }
    },
    MAIN_PAGE(6, FeedContentType.MAIN_PAGE) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new MainPageCardView(ctx);
        }
    },
    NEWS_LIST(7, FeedContentType.NEWS) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new NewsListCardView(ctx);
        }
    },
    FEATURED_IMAGE(8, FeedContentType.FEATURED_IMAGE) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new FeaturedImageCardView(ctx);
        }
    },
    BECAUSE_YOU_READ_ITEM(9),
    MOST_READ_ITEM(10),
    NEWS_ITEM(11),
    NEWS_ITEM_LINK(12),
    ANNOUNCEMENT(13) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new AnnouncementCardView(ctx);
        }
    },
    SURVEY(14) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new AnnouncementCardView(ctx);
        }
    },
    FUNDRAISING(15) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new AnnouncementCardView(ctx);
        }
    },
    //OFFLINE_COMPILATION(16)
    ONBOARDING_OFFLINE(17) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new AnnouncementCardView(ctx);
        }
    },
    ON_THIS_DAY(18, FeedContentType.ON_THIS_DAY) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new OnThisDayCardView(ctx);
        }
    },
    ONBOARDING_CUSTOMIZE_FEED(19) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new AnnouncementCardView(ctx);
        }
    },
    ONBOARDING_READING_LIST_SYNC(20) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new AnnouncementCardView(ctx);
        }
    },
    DAY_HEADER(97) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new DayHeaderCardView(ctx);
        }
    },
    OFFLINE(98) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new OfflineCardView(ctx);
        }
    },
    PROGRESS(99) {
        @NonNull @Override public FeedCardView<?> newView(@NonNull Context ctx) {
            return new ProgressCardView(ctx);
        }
    };

    private static final EnumCodeMap<CardType> MAP = new EnumCodeMap<>(CardType.class);
    private final int code;
    private FeedContentType contentType;

    @NonNull public static CardType of(int code) {
        return MAP.get(code);
    }

    @NonNull public FeedCardView<?> newView(@NonNull Context ctx) {
        throw new UnsupportedOperationException();
    }

    @Override public int code() {
        return code;
    }

    public FeedContentType contentType() {
        return contentType;
    }

    CardType(int code) {
        this(code, null);
    }

    CardType(int code, FeedContentType contentType) {
        this.code = code;
        this.contentType = contentType;
    }
}

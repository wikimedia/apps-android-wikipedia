package org.wikipedia.feed.model

import android.content.Context
import org.wikipedia.feed.FeedContentType
import org.wikipedia.feed.accessibility.AccessibilityCardView
import org.wikipedia.feed.announcement.AnnouncementCardView
import org.wikipedia.feed.becauseyouread.BecauseYouReadCardView
import org.wikipedia.feed.dayheader.DayHeaderCardView
import org.wikipedia.feed.featured.FeaturedArticleCardView
import org.wikipedia.feed.image.FeaturedImageCardView
import org.wikipedia.feed.mainpage.MainPageCardView
import org.wikipedia.feed.mostread.MostReadCardView
import org.wikipedia.feed.news.NewsCardView
import org.wikipedia.feed.offline.OfflineCardView
import org.wikipedia.feed.onthisday.OnThisDayCardView
import org.wikipedia.feed.progress.ProgressCardView
import org.wikipedia.feed.random.RandomCardView
import org.wikipedia.feed.searchbar.SearchCardView
import org.wikipedia.feed.suggestededits.SuggestedEditsCardView
import org.wikipedia.feed.view.FeedCardView
import org.wikipedia.model.EnumCode
import org.wikipedia.model.EnumCodeMap

enum class CardType constructor(private val code: Int,
                                private val contentType: FeedContentType? = null) : EnumCode {
    SEARCH_BAR(0) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return SearchCardView(ctx)
        }
    },
    BECAUSE_YOU_READ_LIST(2, FeedContentType.BECAUSE_YOU_READ) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return BecauseYouReadCardView(ctx)
        }
    },
    MOST_READ_LIST(3, FeedContentType.MOST_READ_ARTICLES) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return MostReadCardView(ctx)
        }
    },
    FEATURED_ARTICLE(4, FeedContentType.FEATURED_ARTICLE) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return FeaturedArticleCardView(ctx)
        }
    },
    RANDOM(5, FeedContentType.RANDOM) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return RandomCardView(ctx)
        }
    },
    MAIN_PAGE(6, FeedContentType.MAIN_PAGE) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return MainPageCardView(ctx)
        }
    },
    NEWS_LIST(7, FeedContentType.NEWS) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return NewsCardView(ctx)
        }
    },
    FEATURED_IMAGE(8, FeedContentType.FEATURED_IMAGE) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return FeaturedImageCardView(ctx)
        }
    },
    BECAUSE_YOU_READ_ITEM(9), MOST_READ_ITEM(10), NEWS_ITEM(11), NEWS_ITEM_LINK(12), ANNOUNCEMENT(13) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return AnnouncementCardView(ctx)
        }
    },
    SURVEY(14) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return AnnouncementCardView(ctx)
        }
    },
    FUNDRAISING(15) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return AnnouncementCardView(ctx)
        }
    },
    ONBOARDING_OFFLINE(17) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return AnnouncementCardView(ctx)
        }
    },
    ON_THIS_DAY(18, FeedContentType.ON_THIS_DAY) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return OnThisDayCardView(ctx)
        }
    },
    ONBOARDING_CUSTOMIZE_FEED(19) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return AnnouncementCardView(ctx)
        }
    },
    SUGGESTED_EDITS(21, FeedContentType.SUGGESTED_EDITS) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return SuggestedEditsCardView(ctx)
        }
    },
    ACCESSIBILITY(22, FeedContentType.ACCESSIBILITY) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return AccessibilityCardView(ctx)
        }
    },
    // TODO: refactor this item when the new Modern Event Platform is finished.
    ARTICLE_ANNOUNCEMENT(96) {
        override fun newView(ctx: Context): FeedCardView<*> {
            // This is not actually used, since this type of card will not be shown in the feed.
            return AnnouncementCardView(ctx)
        }
    },
    DAY_HEADER(97) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return DayHeaderCardView(ctx)
        }
    },
    OFFLINE(98) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return OfflineCardView(ctx)
        }
    },
    PROGRESS(99) {
        override fun newView(ctx: Context): FeedCardView<*> {
            return ProgressCardView(ctx)
        }
    };

    override fun code(): Int {
        return code
    }

    open fun newView(ctx: Context): FeedCardView<*> {
        throw UnsupportedOperationException()
    }

    fun contentType(): FeedContentType? {
        return contentType
    }

    companion object {
        private val MAP = EnumCodeMap(CardType::class.java)

        fun of(code: Int): CardType {
            return MAP[code]
        }
    }
}

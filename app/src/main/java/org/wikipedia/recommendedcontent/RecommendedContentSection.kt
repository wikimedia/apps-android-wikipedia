package org.wikipedia.recommendedcontent

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wikipedia.R
import org.wikipedia.model.EnumCode

@Suppress("unused")
enum class RecommendedContentSection constructor(val id: Int,
                                                 val viewId: Int,
                                                 @StringRes val titleResId: Int) : EnumCode {
    TOP_READ(0, R.id.recommended_content_section_top_read, R.string.recommended_content_section_top_read) {
        override fun select(cb: Callback) {
        }
    },
    EXPLORE(1, R.id.recommended_content_section_explore, R.string.recommended_content_section_explore) {
        override fun select(cb: Callback) {
        }
    },
    ON_THIS_DAY(2, R.id.recommended_content_section_on_this_day, R.string.recommended_content_section_on_this_day) {
        override fun select(cb: Callback) {
        }
    },
    IN_THE_NEWS(3, R.id.recommended_content_section_in_the_news, R.string.recommended_content_section_in_the_news) {
        override fun select(cb: Callback) {
        }
    },
    PLACES_NEAR_YOU(4, R.id.recommended_content_section_places_near_you, R.string.recommended_content_section_places_near_you) {
        override fun select(cb: Callback) {
        }
    },
    BECAUSE_YOU_READ(5, R.id.recommended_content_section_because_you_read, R.string.recommended_content_section_because_you_read) {
        override fun select(cb: Callback) {
        }
    },
    CONTINUE_READING(6, R.id.recommended_content_section_continue_reading, R.string.recommended_content_section_continue_reading) {
        override fun select(cb: Callback) {
        }
    },
    RANDOM(0, R.id.recommended_content_section_random, R.string.recommended_content_section_random) {
        override fun select(cb: Callback) {
        }
    };

    abstract fun select(cb: Callback)

    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal
    }

    interface Callback {
        // TODO: implement
    }

    companion object {

    }
}

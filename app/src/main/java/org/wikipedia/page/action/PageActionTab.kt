package org.wikipedia.page.action

import org.wikipedia.model.EnumCode
import org.wikipedia.model.EnumCodeMap

@Suppress("unused")
enum class PageActionTab : EnumCode {
    ADD_TO_READING_LIST {
        override fun select(cb: Callback) {
            cb.onAddToReadingListTabSelected()
        }
    },
    CHOOSE_LANGUAGE {
        override fun select(cb: Callback) {
            cb.onChooseLangTabSelected()
        }
    },
    FIND_IN_PAGE {
        override fun select(cb: Callback) {
            cb.onFindInPageTabSelected()
        }
    },
    FONT_AND_THEME {
        override fun select(cb: Callback) {
            cb.onFontAndThemeTabSelected()
        }
    },
    VIEW_TOC {
        override fun select(cb: Callback) {
            cb.onViewToCTabSelected()
        }
    };

    abstract fun select(cb: Callback)
    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal
    }

    interface Callback {
        fun onAddToReadingListTabSelected()
        fun onChooseLangTabSelected()
        fun onFindInPageTabSelected()
        fun onFontAndThemeTabSelected()
        fun onViewToCTabSelected()
        fun updateBookmark(pageSaved: Boolean)
    }

    companion object {
        private val MAP = EnumCodeMap(PageActionTab::class.java)

        @JvmStatic
        fun of(code: Int): PageActionTab {
            return MAP[code]
        }
    }
}

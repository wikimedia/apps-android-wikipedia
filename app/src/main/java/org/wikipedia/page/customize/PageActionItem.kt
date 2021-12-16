package org.wikipedia.page.customize

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wikipedia.R
import org.wikipedia.model.EnumCode
import org.wikipedia.model.EnumCodeMap

@Suppress("unused")
enum class PageActionItem constructor(val id: Int,
                                      @StringRes val titleResId: Int,
                                      @DrawableRes val iconResId: Int = R.drawable.ic_settings_black_24dp,
                                      val isExternalLink: Boolean = false) : EnumCode {
    SAVE(0, R.string.article_menu_bar_save_button, R.drawable.ic_bookmark_border_white_24dp) {
        override fun select(cb: Callback) {
            cb.onSaveSelected()
        }
    },
    LANGUAGE(1, R.string.article_menu_bar_language_button, R.drawable.ic_translate_white_24dp){
        override fun select(cb: Callback) {
            cb.onLanguageSelected()
        }
    },
    FIND_IN_ARTICLE(2, R.string.menu_page_find_in_page, R.drawable.ic_find_in_page_24px){
        override fun select(cb: Callback) {
            cb.onFindInArticleSelected()
        }
    },
    THEME(3, R.string.article_menu_bar_theme_button, R.drawable.ic_icon_format_size){
        override fun select(cb: Callback) {
            cb.onThemeSelected()
        }
    },
    CONTENTS(4, R.string.article_menu_bar_contents_button, R.drawable.ic_icon_list){
        override fun select(cb: Callback) {
            cb.onContentsSelected()
        }
    },
    SHARE(5, R.string.menu_article_share, R.drawable.ic_share){
        override fun select(cb: Callback) {
            cb.onShareSelected()
        }
    },
    ADD_TO_WATCHLIST(6, R.string.menu_page_add_to_watchlist, R.drawable.ic_baseline_star_outline_24){
        override fun select(cb: Callback) {
            cb.onAddToWatchlistSelected()
        }
    },
    VIEW_TALK_PAGE(7, R.string.menu_page_view_talk, R.drawable.ic_icon_speech_bubbles_ooui_ltr){
        override fun select(cb: Callback) {
            cb.onViewTalkPageSelected()
        }
    },
    VIEW_EDIT_HISTORY(8, R.string.menu_page_view_edit_history, R.drawable.ic_icon_revision_history_apps, true){
        override fun select(cb: Callback) {
            cb.onViewEditHistorySelected()
        }
    },
    NEW_TAB(9, R.string.menu_new_tab, R.drawable.ic_add_gray_white_24dp){
        override fun select(cb: Callback) {
            cb.onNewTabSelected()
        }
    },
    EXPLORE(10, R.string.feed, R.drawable.ic_globe){
        override fun select(cb: Callback) {
            cb.onExploreSelected()
        }
    };

    abstract fun select(cb: Callback)

    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal
    }

    interface Callback {
        fun onSaveSelected()
        fun onLanguageSelected()
        fun onFindInArticleSelected()
        fun onThemeSelected()
        fun onContentsSelected()
        fun onShareSelected()
        fun onAddToWatchlistSelected()
        fun onViewTalkPageSelected()
        fun onViewEditHistorySelected()
        fun onNewTabSelected()
        fun onExploreSelected()
        fun updateBookmark(pageSaved: Boolean)
        fun updateWatchlist(pageWatched: Boolean)
    }

    companion object {
        val MAP = EnumCodeMap(PageActionItem::class.java)

        fun size(): Int {
            return MAP.size()
        }

        private fun findOrNull(id: Int): PageActionItem? {
            return MAP.valueIterator().asSequence().firstOrNull { id == it.id }
        }

        fun find(id: Int): PageActionItem {
            return findOrNull(id) ?: MAP[0]
        }
    }
}

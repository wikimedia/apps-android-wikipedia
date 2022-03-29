package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.analytics.eventplatform.ArticleInteractionEvent.ActionType.*
import org.wikipedia.auth.AccountUtil

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/android_article_toolbar_interaction/1.0.0")
class ArticleInteractionEvent(@SerialName("wiki_db") private var wikiDb: String,
                              @SerialName("page_id") private var pageId: Int) : TimedEvent(STREAM_NAME) {

    @SerialName("time_spent_ms") private var timeSpentMs: Int? = null
    @SerialName("is_anon") private var isAnon: Boolean? = null
    private var action: String? = null

    fun logSaveClick() {
        action = SAVE.valueString
        submitEvent()
    }

    fun logLanguageClick() {
        action = LANGUAGE.valueString
        submitEvent()
    }

    fun logFindInArticleClick() {
        action = FIND_IN_ARTICLE.valueString
        submitEvent()
    }

    fun logThemeClick() {
        action = THEME.valueString
        submitEvent()
    }

    fun logContentsClick() {
        action = CONTENTS.valueString
        submitEvent()
    }

    fun logMoreClick() {
        action = MORE.valueString
        submitEvent()
    }

    fun logShareClick() {
        action = SHARE.valueString
        submitEvent()
    }

    fun logTalkPageClick() {
        action = TALK_PAGE.valueString
        submitEvent()
    }

    fun logEditHistoryClick() {
        action = EDIT_HISTORY.valueString
        submitEvent()
    }

    fun logNewTabClick() {
        action = NEW_TAB.valueString
        submitEvent()
    }

    fun logExploreClick() {
        action = EXPLORE.valueString
        submitEvent()
    }

    fun logForwardClick() {
        action = FORWARD.valueString
        submitEvent()
    }

    fun logNotificationClick() {
        action = NOTIFICATION.valueString
        submitEvent()
    }

    fun logTabsClick() {
        action = TABS.valueString
        submitEvent()
    }

    fun logSearchWikipediaClick() {
        action = SEARCH_WIKIPEDIA.valueString
        submitEvent()
    }

    fun logBackClick() {
        action = BACK.valueString
        submitEvent()
    }

    fun logEditHistoryArticleClick() {
        action = EDIT_HISTORY_ARTICLE.valueString
        submitEvent()
    }

    fun logTalkPageArticleClick() {
        action = TALK_PAGE_ARTICLE.valueString
        submitEvent()
    }

    fun logTocSwipe() {
        action = TOC_SWIPE.valueString
        submitEvent()
    }

    fun logCategoriesClick() {
        action = CATEGORIES.valueString
        submitEvent()
    }

    private fun submitEvent() {
        timeSpentMs = duration.toInt()
        isAnon = !AccountUtil.isLoggedIn
        EventPlatformClient.submit(this)
    }

    companion object {
        private const val STREAM_NAME = "android.article_toolbar_interaction"
    }

    enum class ActionType(val valueString: String) {
        SAVE("save"),
        LANGUAGE("language"),
        FIND_IN_ARTICLE("find_in_article"),
        THEME("theme"),
        CONTENTS("contents"),
        MORE("more"),
        SHARE("share"),
        TALK_PAGE("talk_page"),
        EDIT_HISTORY("edit_history"),
        CATEGORIES("categories"),
        NEW_TAB("new_tab"),
        EXPLORE("explore"),
        NOTIFICATION("notification"),
        TABS("tabs"),
        SEARCH_WIKIPEDIA("search_wikipedia"),
        BACK("back"),
        FORWARD("forward"),
        EDIT_HISTORY_ARTICLE("edit_history_from_article"),
        TALK_PAGE_ARTICLE("talk_page_from_article"),
        TOC_SWIPE("toc_swipe");
    }
}

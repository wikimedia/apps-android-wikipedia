package org.wikipedia.page.pageload

import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle

data class PageLoadRequest(
    val title: PageTitle,
    val entry: HistoryEntry,
    val options: PageLoadOptions = PageLoadOptions()
)

data class PageLoadOptions(
    val pushBackStack: Boolean = true,
    val squashBackStack: Boolean = false,
    val isRefresh: Boolean = false,
    val stagedScrollY: Int = 0,
    val shouldLoadFromBackStack: Boolean = false,
    val tabPosition: PageActivity.TabPosition = PageActivity.TabPosition.CURRENT_TAB
)

sealed class LoadType {
    object CurrentTab : LoadType()
    object NewForegroundTab : LoadType()
    object NewBackgroundTab : LoadType()
    object ExistingTab : LoadType()
    object FromBackStack : LoadType()
}

sealed class PageLoadUiState {
    data class SpecialPage(val request: PageLoadRequest) : PageLoadUiState()
    data class LoadingPrep(val isRefresh: Boolean = false, val title: PageTitle? = null) : PageLoadUiState()
    data class Success(
        val result: PageSummary? = null,
        val title: PageTitle,
        val stagedScrollY: Int = 0,
        val sectionAnchor: String? = null,
        val redirectedFrom: String?) : PageLoadUiState()
    data class Error(val throwable: Throwable) : PageLoadUiState()
}

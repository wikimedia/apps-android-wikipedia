package org.wikipedia.page.pageload

import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.PageViewModel

data class PageLoadRequest(
    val title: PageTitle,
    val entry: HistoryEntry,
    val options: PageLoadOptions = PageLoadOptions()
)

data class PageLoadOptions(
    val pushbackStack: Boolean = true,
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
    data class Loading(val isRefresh: Boolean = false) : PageLoadUiState()
    data class Success(
        val result: PageResult.Success? = null,
        val title: PageTitle,
        val stagedScrollY: Int = 0,
        val loadedFromBackground: Boolean = false,
        val sectionAnchor: String? = null) : PageLoadUiState()
    data class Error(val throwable: Throwable) : PageLoadUiState()
}

package org.wikipedia.page.pageload

import org.wikipedia.categories.db.Category
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.Page
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import retrofit2.Response

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
    val tabPosition: PageActivity.TabPosition = PageActivity.TabPosition.CURRENT_TAB
)

sealed class LoadType {
    object CurrentTab: LoadType()
    object NewForegroundTab: LoadType()
    object NewBackgroundTab: LoadType()
    object ExistingTab: LoadType()
    data class WithScrollPosition(val scrollY: Int): LoadType()
}

sealed class LoadState {
    object Idle: LoadState()
    object Loading: LoadState()
    data class Success(val result: PageResult? = null): LoadState()
    data class Error(val throwable: Throwable): LoadState()
}
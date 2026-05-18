package org.wikipedia.notifications

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.wikipedia.util.Resource

// This interface allows using both NotificationRefactoredViewModel and NotificationLegacyViewModel
// in one unit test class
interface NotificationViewModel {
    fun fetchAndSave(refresh: Boolean = false)
    fun updateSearchQuery(query: String?)
    val uiState: StateFlow<Resource<Pair<List<NotificationListItemContainer>, Boolean>>>
    fun updateTabSelection(position: Int)

    var mentionsUnreadCount: Int

    var allUnreadCount: Int
}
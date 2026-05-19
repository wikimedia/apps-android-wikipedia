package org.wikipedia.notifications

/**
 * Interface alllowing use of both NotificationRefactoredViewModel and NotificationLegacyViewModel
 * in unit test [NotificationViewModelTest]
 */

interface NotificationViewModel {
    fun fetchAndSave(refresh: Boolean = false)
    fun updateSearchQuery(query: String?)

    fun updateTabSelection(position: Int)

    var mentionsUnreadCount: Int

    var allUnreadCount: Int
}

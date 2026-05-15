package org.wikipedia.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating [NotificationRefactoredViewModel] instances with required dependencies.
 */
class NotificationViewModelFactory(
    private val notificationPreferences: NotificationPreferences,
    private val notificationRepository: NotificationRepository,
    private val notificationFilterHelper: NotificationFilterHelper
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NotificationRefactoredViewModel(
            notificationPreferences,
            notificationRepository,
            notificationFilterHelper
        ) as T
    }
}
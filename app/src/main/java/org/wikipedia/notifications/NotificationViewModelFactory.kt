package org.wikipedia.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating [NotificationViewModel] instances with required dependencies.
 */
class NotificationViewModelFactory(
    private val preferences: NotificationPreferences,
    private val notificationRepository: NotificationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NotificationViewModel(preferences, notificationRepository) as T
    }
}
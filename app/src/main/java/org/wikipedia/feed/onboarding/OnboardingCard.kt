package org.wikipedia.feed.onboarding

import androidx.annotation.StringRes
import org.wikipedia.feed.announcement.Announcement
import org.wikipedia.feed.announcement.AnnouncementCard
import org.wikipedia.settings.PrefsIoUtil

abstract class OnboardingCard(announcement: Announcement) : AnnouncementCard(announcement) {

    @StringRes abstract fun prefKey(): Int

    open fun shouldShow(): Boolean {
        return PrefsIoUtil.getBoolean(prefKey(), true)
    }

    override fun onDismiss() {
        PrefsIoUtil.setBoolean(prefKey(), false)
    }

    override fun onRestore() {
        PrefsIoUtil.setBoolean(prefKey(), true)
    }
}

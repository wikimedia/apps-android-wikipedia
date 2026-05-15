package org.wikipedia.activity

import org.wikipedia.feed.onboarding.ExploreFeedBuildingActivity
import org.wikipedia.feed.onboarding.ExploreFeedUpdatePromptActivity
import org.wikipedia.feed.personalization.PersonalizationActivity
import org.wikipedia.onboarding.InitialOnboardingActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.widgets.readingchallenge.ReadingChallengeOnboardingActivity
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetRepository
import org.wikipedia.yearinreview.YearInReviewActivity
import org.wikipedia.yearinreview.YearInReviewOnboardingActivity
import org.wikipedia.yearinreview.YearInReviewViewModel

object AppAnnouncements {
    // Announcements must never show while these activities are on screen.
    // They all extend BaseActivity, so without this guard, launching any of them
    // would re-trigger maybeShowAnnouncement and could show an announcement on top of or immediately after
    // an onboarding or announcement screen. This can cause user to see announcements back to back.
    private val blockedActivities = setOf(
        InitialOnboardingActivity::class.java,
        ExploreFeedBuildingActivity::class.java,
        ExploreFeedUpdatePromptActivity::class.java,
        PersonalizationActivity::class.java,
        YearInReviewOnboardingActivity::class.java,
        YearInReviewActivity::class.java,
        ReadingChallengeOnboardingActivity::class.java
    )

    fun maybeShowAnnouncement(activity: BaseActivity) {
        if (activity::class.java in blockedActivities) return
        if (Prefs.isInitialOnboardingEnabled) return
        if (!Prefs.isExploreFeedUpdatePromptShown) return

        when {
            shouldShowReadingChallenge() -> activity.showReadingChallenge()
            shouldShowYearInReview() -> activity.showYearInReview()
        }
    }

    private fun shouldShowYearInReview() = YearInReviewViewModel.isAccessible &&
            Prefs.isYearInReviewEnabled &&
            !Prefs.yearInReviewVisited

    private fun shouldShowReadingChallenge() = ReadingChallengeWidgetRepository.shouldShowOnboardingDialog()
}

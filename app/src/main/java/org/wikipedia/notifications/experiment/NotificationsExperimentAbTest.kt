package org.wikipedia.notifications.experiment

import android.content.Context
import org.wikimedia.testkitchen.instrument.InstrumentImpl
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ABTest
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.readinglist.recommended.RecommendedReadingListNotificationManager
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.ReleaseUtil

class NotificationsExperimentAbTest : ABTest("discover-notifications-test-1", GROUP_SIZE_2) {
    override fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "treatment"
            else -> "control"
        }
    }

    private fun isTestActive(): Boolean {
        return Prefs.notificationsIsTestActive || RemoteConfig.config.androidv1?.notificationsExperimentEnabledv1 ?: false
    }

    private fun isTestGroupUser(): Boolean {
        return group != GROUP_1
    }

    private fun isLanguageSupported(languageCode: String?): Boolean {
        return ReleaseUtil.isPreBetaRelease || supportedLanguages.any { it.equals(languageCode, true) }
    }

    private val supportedLanguages = RemoteConfig.config.androidv1?.notificationsExperimentLanguagesv1.orEmpty()

    fun isNotificationsExperimentEligible(): Boolean {
        // Users who already entered the experiment stay in it even if they later switch to an unsupported language.
        val isLanguageEligible = Prefs.isNotificationsExperimentNotificationRescheduled || isLanguageSupported(WikipediaApp.instance.languageState.appLanguageCode)
        return isTestActive() && isLanguageEligible && Prefs.isRecommendedReadingListNotificationEnabled
    }

    fun isNotificationsExperimentEnabled(): Boolean {
        return isTestGroupUser() && isNotificationsExperimentEligible()
    }

    fun runIfEligible(function: () -> Unit) {
        if (isNotificationsExperimentEligible()) {
            function.invoke()
        }
    }

    fun maybeSendExposureEventIfEligible() {
        runIfEligible {
            maybeSendExposureEvent()
        }
    }

    fun setExperimentIfEligible(instrument: InstrumentImpl) {
        runIfEligible {
            instrument.setExperiment(TestKitchenAdapter.getExperiment(this))
        }
    }

    // TODO: reschedule for the experiment (to weekly), and make sure reschedule again when the experiment is ended.
    fun maybeRescheduleRecommendedReadingListNotification(context: Context) {
        if (Prefs.isNotificationsExperimentNotificationRescheduled || !isNotificationsExperimentEligible()) {
            return
        }
        RecommendedReadingListNotificationManager.scheduleRecommendedReadingListNotification(context)
        Prefs.isNotificationsExperimentNotificationRescheduled = true
    }
}

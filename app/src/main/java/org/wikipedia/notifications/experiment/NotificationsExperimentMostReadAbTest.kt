package org.wikipedia.notifications.experiment

import org.wikimedia.testkitchen.instrument.InstrumentImpl
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ABTest
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.games.onthisday.OnThisDayGameNotificationState
import org.wikipedia.readinglist.recommended.RecommendedReadingListUpdateFrequency
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.ReleaseUtil

class NotificationsExperimentMostReadAbTest : ABTest("trending-notifications-test-2", GROUP_SIZE_2) {
    override fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "treatment"
            else -> "control"
        }
    }

    private fun isTestActive(): Boolean {
        return Prefs.notificationsMostReadIsTestActive || RemoteConfig.config.androidv1?.notificationsExperimentEnabledv2 ?: false
    }

    private fun isTestGroupUser(): Boolean {
        return group != GROUP_1
    }

    private fun isLanguageSupported(languageCode: String?): Boolean {
        return ReleaseUtil.isPreBetaRelease || supportedLanguages.any { it.equals(languageCode, true) }
    }

    private val supportedLanguages = RemoteConfig.config.androidv1?.notificationsExperimentLanguagesv2.orEmpty()

    fun isNotificationsExperimentEligible(): Boolean {
        // Users who already entered the experiment stay in it even if they later switch to an unsupported language.
        val isLanguageEligible = Prefs.notificationsExperimentWeeklyMostReadScheduled || isLanguageSupported(WikipediaApp.instance.languageState.appLanguageCode)
        return isTestActive() && isLanguageEligible
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
            // Additional event to compare with the experiment 1
            val isGameNotificationEnabled = Prefs.otdNotificationState == OnThisDayGameNotificationState.ENABLED
            val isDiscoverNotificationEnabled = Prefs.isRecommendedReadingListNotificationEnabled
            val frequencyForEvent = when (Prefs.recommendedReadingListUpdateFrequency) {
                RecommendedReadingListUpdateFrequency.DAILY -> "daily"
                RecommendedReadingListUpdateFrequency.WEEKLY -> "weekly"
                RecommendedReadingListUpdateFrequency.MONTHLY -> "monthly"
            }
            val actionContext = buildMap<String, Any> {
                put("game", isGameNotificationEnabled)
                put("discover", isDiscoverNotificationEnabled)
                if (isDiscoverNotificationEnabled) {
                    put("discover_freq", frequencyForEvent)
                }
            }
            TestKitchenAdapter.client.getInstrument("apps-notifications")
                .also { setExperimentIfEligible(it) }
                .submitInteraction(action = "notification_status", actionContext = actionContext)
        }
    }

    fun setExperimentIfEligible(instrument: InstrumentImpl) {
        runIfEligible {
            instrument.setExperiment(TestKitchenAdapter.getExperiment(this))
        }
    }
}

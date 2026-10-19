package org.wikipedia.notifications.experiment

import org.wikimedia.testkitchen.instrument.InstrumentImpl
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ABTest
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.ReleaseUtil

class NotificationsExperimentAbTest : ABTest("notifications-experiment-1", GROUP_SIZE_2) {
    override fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "treatment"
            else -> "control"
        }
    }

    private fun isTestActive(): Boolean {
        return Prefs.notificationsIsTestActive || RemoteConfig.config.androidv1?.notificationsExperimentEnabled ?: false
    }

    private fun isTestGroupUser(): Boolean {
        return group != GROUP_1
    }

    private fun isLanguageSupported(languageCode: String?): Boolean {
        return ReleaseUtil.isPreBetaRelease || supportedLanguages.any { it.equals(languageCode, true) }
    }

    private val supportedLanguages = RemoteConfig.config.androidv1?.notificationsExperimentLanguages.orEmpty()

    fun isNotificationsExperimentEligible(): Boolean {
        return isTestActive() && isLanguageSupported(WikipediaApp.instance.languageState.appLanguageCode) && Prefs.isRecommendedReadingListNotificationEnabled
    }

    fun isNotificationsExperimentEnabled(): Boolean {
        return isTestGroupUser() && isNotificationsExperimentEligible()
    }

    fun shouldSendEvent(function: () -> Unit) {
        if (isNotificationsExperimentEligible()) {
            function.invoke()
        }
    }

    fun setExperimentIfEligible(instrument: InstrumentImpl) {
        shouldSendEvent {
            instrument.setExperiment(TestKitchenAdapter.getExperiment(this))
        }
    }
}

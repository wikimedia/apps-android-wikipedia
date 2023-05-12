package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.MetricsClient
import org.wikimedia.metrics_platform.context.AgentData
import org.wikimedia.metrics_platform.context.ClientData
import org.wikimedia.metrics_platform.context.MediawikiData
import org.wikimedia.metrics_platform.context.PerformerData
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil
import java.util.Random

object MetricsPlatform {
    private val agentData: AgentData = AgentData(
        WikipediaApp.instance.appInstallID,
        "mobile app",
        "android"
    )

    private val mediawikiData: MediawikiData = MediawikiData(
        null,
        WikipediaApp.instance.versionCode.toString(),
        ReleaseUtil.isProdRelease,
        ReleaseUtil.isDevRelease,
        WikipediaApp.instance.wikiSite.dbName(),
        WikipediaApp.instance.languageState.systemLanguageCode,
        WikipediaApp.instance.languageState.appLanguageLocalizedNames
    )

    private val performerData: PerformerData = PerformerData(
        AccountUtil.userName,
        AccountUtil.isLoggedIn,
        AccountUtil.hashCode(),
        AssociationController.sessionId,
        AssociationController.pageViewId,
        AccountUtil.groups,
        null,
        WikipediaApp.instance.languageState.appLanguageCode,
        WikipediaApp.instance.languageState.remainingAvailableLanguageCodes.toString(),
        null,
        null,
        null,
        null
    )

    private val clientData: ClientData = ClientData(
        agentData,
        mediawikiData,
        performerData,
        WikipediaApp.instance.wikiSite.authority()
    )

    val client: MetricsClient = MetricsClient.builder(clientData).build()

    /**
     * AssociationController: provides associative identifiers and manage their
     * persistence
     *
     * Identifiers correspond to various scopes e.g. 'pageview', 'session', and 'device'.
     *
     * TODO: Possibly get rid of the pageview type?  Does it make sense on apps?  It is not in the iOS library currently.
     * On apps, a "session" starts when the app is loaded, and ends when completely closed, or after 15 minutes of inactivity
     * Save a ts when going into bg, then when returning to foreground, & if it's been more than 15 mins, start a new session, else continue session from before
     * Possible to query/track time since last interaction? (For future)
     */
    internal object AssociationController {
        private var PAGEVIEW_ID: String? = null
        private var SESSION_ID: String? = null

        /**
         * Generate a pageview identifier.
         *
         * @return pageview ID
         *
         * The identifier is a string of 20 zero-padded hexadecimal digits
         * representing a uniformly random 80-bit integer.
         */
        val pageViewId: String
            get() {
                if (PAGEVIEW_ID == null) {
                    PAGEVIEW_ID = generateRandomId()
                }
                return PAGEVIEW_ID!!
            }

        /**
         * Generate a session identifier.
         *
         * @return session ID
         *
         * The identifier is a string of 20 zero-padded hexadecimal digits
         * representing a uniformly random 80-bit integer.
         */
        val sessionId: String
            get() {
                if (SESSION_ID == null) {
                    // If there is no runtime value for SESSION_ID, try to load a
                    // value from persistent store.
                    SESSION_ID = Prefs.eventPlatformSessionId
                    if (SESSION_ID == null) {
                        // If there is no value in the persistent store, generate a new value for
                        // SESSION_ID, and write the update to the persistent store.
                        SESSION_ID = generateRandomId()
                        Prefs.eventPlatformSessionId = SESSION_ID
                    }
                }
                return SESSION_ID!!
            }

        fun beginNewSession() {
            // Clear runtime and persisted value for SESSION_ID.
            SESSION_ID = null
            Prefs.eventPlatformSessionId = null

            // A session refresh implies a pageview refresh, so clear runtime value of PAGEVIEW_ID.
            beginNewPageView()
        }

        fun beginNewPageView() {
            PAGEVIEW_ID = null
        }

        /**
         * @return a string of 20 zero-padded hexadecimal digits representing a uniformly random
         * 80-bit integer
         */
        private fun generateRandomId(): String {
            val random = Random()
            return String.format("%08x", random.nextInt()) + String.format("%08x", random.nextInt()) + String.format("%04x", random.nextInt() and 0xFFFF)
        }
    }
}

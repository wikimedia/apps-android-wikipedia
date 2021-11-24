package org.wikipedia.feed.announcement

import android.content.Context
import androidx.annotation.VisibleForTesting
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.feed.model.Card
import org.wikipedia.settings.Prefs
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.log.L
import java.util.*

class AnnouncementClient : FeedClient {

    private val disposables = CompositeDisposable()

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        disposables.add(ServiceFactory.getRest(wiki).announcements
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ list ->
                FeedCoordinator.postCardsToCallback(cb, buildCards(list.items))
            }) { throwable ->
                L.v(throwable)
                cb.error(throwable)
            })
    }

    override fun cancel() {
        disposables.clear()
    }

    companion object {
        private const val PLATFORM_CODE = "AndroidApp"
        private const val PLATFORM_CODE_NEW = "AndroidAppV2"

        @VisibleForTesting
        private fun buildCards(announcements: List<Announcement>): List<Card> {
            val cards = mutableListOf<Card>()
            val country = GeoUtil.geoIPCountry
            val now = Date()
            for (announcement in announcements) {
                if (shouldShow(announcement, country, now)) {
                    when (announcement.type) {
                        Announcement.SURVEY -> cards.add(SurveyCard(announcement))
                        Announcement.FUNDRAISING -> if (announcement.placement == Announcement.PLACEMENT_FEED) {
                            cards.add(FundraisingCard(announcement))
                        }
                        else -> cards.add(AnnouncementCard(announcement))
                    }
                }
            }
            return cards
        }

        @JvmStatic
        fun shouldShow(announcement: Announcement?, country: String?, date: Date): Boolean {
            return (announcement != null && !announcement.platforms.isNullOrEmpty() && (announcement.platforms.contains(PLATFORM_CODE) ||
                    announcement.platforms.contains(PLATFORM_CODE_NEW)) &&
                    matchesCountryCode(announcement, country) && matchesDate(announcement, date) &&
                    matchesVersionCodes(announcement.minVersion(), announcement.maxVersion()) && matchesConditions(announcement))
        }

        private fun matchesCountryCode(announcement: Announcement, country: String?): Boolean {
            var countryCode = country
            val announcementsCountryOverride = Prefs.announcementsCountryOverride
            if (!announcementsCountryOverride.isNullOrEmpty()) {
                countryCode = announcementsCountryOverride
            }
            return if (countryCode.isNullOrEmpty() || announcement.countries.isNullOrEmpty()) {
                false
            } else announcement.countries.contains(countryCode)
        }

        private fun matchesDate(announcement: Announcement, date: Date): Boolean {
            if (Prefs.ignoreDateForAnnouncements) {
                return true
            }
            return announcement.startTime()?.before(date) == true && announcement.endTime()?.after(date) == true
        }

        private fun matchesConditions(announcement: Announcement): Boolean {
            if (announcement.beta != null && announcement.beta != ReleaseUtil.isPreProdRelease) {
                return false
            }
            return if (announcement.loggedIn != null && announcement.loggedIn != AccountUtil.isLoggedIn) {
                false
            } else announcement.readingListSyncEnabled == null || announcement.readingListSyncEnabled == Prefs.isReadingListSyncEnabled
        }

        private fun matchesVersionCodes(minVersion: Int, maxVersion: Int): Boolean {
            val versionCode = if (Prefs.announcementsVersionCode > 0) Prefs.announcementsVersionCode
            else WikipediaApp.getInstance().versionCode
            try {
                if (minVersion != -1 && minVersion > versionCode) {
                    return false
                }
                if (maxVersion != -1 && maxVersion < versionCode) {
                    return false
                }
            } catch (e: NumberFormatException) {
                // ignore
            }
            return true
        }
    }
}

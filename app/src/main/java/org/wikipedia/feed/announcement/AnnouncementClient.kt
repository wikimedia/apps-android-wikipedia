package org.wikipedia.feed.announcement

import android.content.Context
import org.wikipedia.util.log.L.v
import org.wikipedia.auth.AccountUtil.isLoggedIn
import org.wikipedia.feed.dataclient.FeedClient
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.ServiceFactory
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.wikipedia.feed.announcement.AnnouncementList
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.announcement.AnnouncementClient
import org.wikipedia.util.log.L
import org.wikipedia.feed.announcement.Announcement
import org.wikipedia.util.GeoUtil
import org.wikipedia.feed.announcement.SurveyCard
import org.wikipedia.feed.announcement.FundraisingCard
import org.wikipedia.feed.announcement.AnnouncementCard
import org.wikipedia.settings.Prefs
import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.auth.AccountUtil
import org.wikipedia.WikipediaApp
import org.wikipedia.feed.model.Card
import java.lang.NumberFormatException
import java.util.*

class AnnouncementClient : FeedClient {
    private val disposables = CompositeDisposable()
    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        disposables.add(ServiceFactory.getRest(wiki).announcements
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ list: AnnouncementList -> FeedCoordinator.postCardsToCallback(cb, ArrayList(buildCards(list.items()))) }) { throwable: Throwable? ->
                    v(throwable)
                    cb.error(throwable!!)
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
            val cards: MutableList<Card> = ArrayList()
            val country = GeoUtil.getGeoIPCountry()
            val now = Date()
            for (announcement in announcements) {
                if (shouldShow(announcement, country, now)) {
                    when (announcement.type()) {
                        Announcement.SURVEY -> cards.add(SurveyCard(announcement))
                        Announcement.FUNDRAISING -> if (announcement.placement() == Announcement.PLACEMENT_FEED) {
                            cards.add(FundraisingCard(announcement))
                        }
                        else -> cards.add(AnnouncementCard(announcement))
                    }
                }
            }
            return cards
        }

        @JvmStatic
        fun shouldShow(announcement: Announcement?,
                       country: String?,
                       date: Date): Boolean {
            return (announcement != null && (announcement.platforms().contains(PLATFORM_CODE) || announcement.platforms().contains(PLATFORM_CODE_NEW))
                    && matchesCountryCode(announcement, country)
                    && matchesDate(announcement, date)
                    && matchesVersionCodes(announcement.minVersion(), announcement.maxVersion())
                    && matchesConditions(announcement))
        }

        private fun matchesCountryCode(announcement: Announcement, country: String?): Boolean {
            var country = country
            val announcementsCountryOverride = Prefs.getAnnouncementsCountryOverride()
            if (!TextUtils.isEmpty(announcementsCountryOverride)) {
                country = announcementsCountryOverride
            }
            return if (TextUtils.isEmpty(country)) {
                false
            } else announcement.countries().contains(country)
        }

        private fun matchesDate(announcement: Announcement, date: Date): Boolean {
            if (Prefs.ignoreDateForAnnouncements()) {
                return true
            }
            return if (announcement.startTime() != null && announcement.startTime()!!.after(date)) {
                false
            } else announcement.endTime() == null || !announcement.endTime()!!.before(date)
        }

        private fun matchesConditions(announcement: Announcement): Boolean {
            if (announcement.beta() != null && announcement.beta() != ReleaseUtil.isPreProdRelease()) {
                return false
            }
            return if (announcement.loggedIn() != null && announcement.loggedIn() != isLoggedIn) {
                false
            } else announcement.readingListSyncEnabled() == null || announcement.readingListSyncEnabled() == Prefs.isReadingListSyncEnabled()
        }

        private fun matchesVersionCodes(minVersion: String?, maxVersion: String?): Boolean {
            val versionCode = if (Prefs.announcementsVersionCode() > 0) Prefs.announcementsVersionCode() else WikipediaApp.getInstance().versionCode
            try {
                if (!TextUtils.isEmpty(minVersion) && minVersion!!.toInt() > versionCode) {
                    return false
                }
                if (!TextUtils.isEmpty(maxVersion) && maxVersion!!.toInt() < versionCode) {
                    return false
                }
            } catch (e: NumberFormatException) {
                // ignore
            }
            return true
        }
    }
}
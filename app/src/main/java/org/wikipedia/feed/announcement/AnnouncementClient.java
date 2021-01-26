package org.wikipedia.feed.announcement;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.FeedCoordinator;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.model.Card;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.GeoUtil;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.wikipedia.feed.announcement.Announcement.PLACEMENT_FEED;

public class AnnouncementClient implements FeedClient {
    private static final String PLATFORM_CODE = "AndroidApp";
    private static final String PLATFORM_CODE_NEW = "AndroidAppV2";

    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public void request(@NonNull Context context, @NonNull WikiSite wiki, int age, @NonNull Callback cb) {
        cancel();
        disposables.add(ServiceFactory.getRest(wiki).getAnnouncements()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> FeedCoordinator.postCardsToCallback(cb, new ArrayList<>(buildCards(list.items()))), throwable -> {
                    L.v(throwable);
                    cb.error(throwable);
                }));
    }

    @Override
    public void cancel() {
        disposables.clear();
    }

    @VisibleForTesting
    private static List<Card> buildCards(@NonNull List<Announcement> announcements) {
        List<Card> cards = new ArrayList<>();
        String country = GeoUtil.getGeoIPCountry();
        Date now = new Date();
        for (Announcement announcement : announcements) {
            if (shouldShow(announcement, country, now)) {
                switch (announcement.type()) {
                    case Announcement.SURVEY:
                        cards.add(new SurveyCard(announcement));
                        break;
                    case Announcement.FUNDRAISING:
                        if (announcement.placement().equals(PLACEMENT_FEED)) {
                            cards.add(new FundraisingCard(announcement));
                        }
                        break;
                    default:
                        cards.add(new AnnouncementCard(announcement));
                        break;
                }
            }
        }
        return cards;
    }

    public static boolean shouldShow(@Nullable Announcement announcement,
                                     @Nullable String country,
                                     @NonNull Date date) {
        return announcement != null
                && (announcement.platforms().contains(PLATFORM_CODE) || announcement.platforms().contains(PLATFORM_CODE_NEW))
                && matchesCountryCode(announcement, country)
                && matchesDate(announcement, date)
                && matchesVersionCodes(announcement.minVersion(), announcement.maxVersion())
                && matchesConditions(announcement);
    }

    private static boolean matchesCountryCode(@NonNull Announcement announcement, String country) {
        String announcementsCountryOverride = Prefs.getAnnouncementsCountryOverride();
        if (!TextUtils.isEmpty(announcementsCountryOverride)) {
            country = announcementsCountryOverride;
        }
        if (TextUtils.isEmpty(country)) {
            return false;
        }
        return announcement.countries().contains(country);
    }

    private static boolean matchesDate(@NonNull Announcement announcement, Date date) {
        if (Prefs.ignoreDateForAnnouncements()) {
            return true;
        }
        if (announcement.startTime() != null && announcement.startTime().after(date)) {
            return false;
        }
        return announcement.endTime() == null || !announcement.endTime().before(date);
    }

    private static boolean matchesConditions(@NonNull Announcement announcement) {
        if (announcement.beta() != null && (announcement.beta() != ReleaseUtil.isPreProdRelease())) {
            return false;
        }
        if (announcement.loggedIn() != null && (announcement.loggedIn() != AccountUtil.isLoggedIn())) {
            return false;
        }
        return announcement.readingListSyncEnabled() == null || (announcement.readingListSyncEnabled() == Prefs.isReadingListSyncEnabled());
    }

    private static boolean matchesVersionCodes(@Nullable String minVersion, @Nullable String maxVersion) {
        int versionCode = (Prefs.announcementsVersionCode() > 0)
                ? Prefs.announcementsVersionCode() : WikipediaApp.getInstance().getVersionCode();
        try {
            if (!TextUtils.isEmpty(minVersion) && Integer.parseInt(minVersion) > versionCode) {
                return false;
            }
            if (!TextUtils.isEmpty(maxVersion) && Integer.parseInt(maxVersion) < versionCode) {
                return false;
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        return true;
    }
}
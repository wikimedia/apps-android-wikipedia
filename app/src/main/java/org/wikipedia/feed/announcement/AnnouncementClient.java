package org.wikipedia.feed.announcement;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.dataclient.RestService;
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

import retrofit2.Call;
import retrofit2.Response;

public class AnnouncementClient implements FeedClient {
    private static final String PLATFORM_CODE = "AndroidApp";
    private static final String PLATFORM_CODE_NEW = "AndroidAppV2";

    @Nullable private Call<AnnouncementList> call;

    @Override
    public void request(@NonNull Context context, @NonNull WikiSite wiki, int age, @NonNull Callback cb) {
        cancel();
        call = request(ServiceFactory.getRest(wiki));
        call.enqueue(new CallbackAdapter(cb, true));
    }

    @Override
    public void cancel() {
        if (call == null) {
            return;
        }
        call.cancel();
        call = null;
    }

    @VisibleForTesting
    @NonNull
    Call<AnnouncementList> request(@NonNull RestService service) {
        return service.getAnnouncements();
    }

    @VisibleForTesting
    static class CallbackAdapter implements retrofit2.Callback<AnnouncementList> {
        @NonNull private final Callback cb;
        private final boolean postDelayed;

        CallbackAdapter(@NonNull Callback cb, boolean postDelayed) {
            this.cb = cb;
            this.postDelayed = postDelayed;
        }

        @Override public void onResponse(@NonNull Call<AnnouncementList> call,
                                         @NonNull Response<AnnouncementList> response) {
            List<Card> cards = new ArrayList<>();
            AnnouncementList content = response.body();
            if (content != null) {
                cards.addAll(buildCards(content.items()));
            }
            if (postDelayed) {
                FeedCoordinator.postCardsToCallback(cb, cards);
            } else {
                cb.success(cards);
            }
        }

        @Override public void onFailure(@NonNull Call<AnnouncementList> call, @NonNull Throwable caught) {
            if (call.isCanceled()) {
                return;
            }
            L.v(caught);
            cb.error(caught);
        }
    }

    @VisibleForTesting
    static List<Card> buildCards(@NonNull List<Announcement> announcements) {
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
                        cards.add(new FundraisingCard(announcement));
                        break;
                    default:
                        cards.add(new AnnouncementCard(announcement));
                        break;
                }
            }
        }
        return cards;
    }

    @VisibleForTesting
    static boolean shouldShow(@Nullable Announcement announcement,
                              @Nullable String country,
                              @NonNull Date date) {
        if (announcement == null
                || !(announcement.platforms().contains(PLATFORM_CODE) || announcement.platforms().contains(PLATFORM_CODE_NEW))
                || !matchesCountryCode(announcement, country)
                || !matchesDate(announcement, date)
                || !matchesVersionCodes(announcement.minVersion(), announcement.maxVersion())
                || !matchesConditions(announcement)) {
            return false;
        }
        return true;
    }

    private static boolean matchesCountryCode(@NonNull Announcement announcement, String country) {
        String announcementsCountryOverride = Prefs.getAnnouncementsCountryOverride();
        if (!TextUtils.isEmpty(announcementsCountryOverride)) {
            country = announcementsCountryOverride;
        }
        if (TextUtils.isEmpty(country)) {
            return false;
        }
        if (!announcement.countries().contains(country)) {
            return false;
        }
        return true;
    }

    private static boolean matchesDate(@NonNull Announcement announcement, Date date) {
        if (Prefs.ignoreDateForAnnouncements()) {
            return true;
        }
        if (announcement.startTime() != null && announcement.startTime().after(date)) {
            return false;
        }
        if (announcement.endTime() != null && announcement.endTime().before(date)) {
            return false;
        }
        return true;
    }

    private static boolean matchesConditions(@NonNull Announcement announcement) {
        if (announcement.beta() != null && (announcement.beta() != ReleaseUtil.isPreProdRelease())) {
            return false;
        }
        if (announcement.loggedIn() != null && (announcement.loggedIn() != AccountUtil.isLoggedIn())) {
            return false;
        }
        if (announcement.readingListSyncEnabled() != null && (announcement.readingListSyncEnabled() != Prefs.isReadingListSyncEnabled())) {
            return false;
        }
        return true;
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

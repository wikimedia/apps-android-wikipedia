package org.wikipedia.feed.announcement;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.model.Card;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Headers;

public class AnnouncementClient implements FeedClient {
    private static final String PLATFORM_CODE = "AndroidApp";

    @Nullable private Call<AnnouncementList> call;

    @Override
    public void request(@NonNull Context context, @NonNull WikiSite wiki, int age, @NonNull Callback cb) {
        cancel();
        String endpoint = String.format(Locale.ROOT, Prefs.getRestbaseUriFormat(), wiki.scheme(),
                wiki.authority());
        Retrofit retrofit = RetrofitFactory.newInstance(endpoint, wiki);
        Service service = retrofit.create(Service.class);
        call = request(service);
        call.enqueue(new CallbackAdapter(cb));
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
    interface Service {

        /**
         * Gets a list of announcements that are currently in effect.
         */
        @NonNull
        @Headers("accept: application/json; charset=utf-8; profile=\"https://www.mediawiki.org/wiki/Specs/announcements/0.1.0\"")
        @GET("feed/announcements")
        Call<AnnouncementList> get();
    }

    @VisibleForTesting
    @NonNull
    Call<AnnouncementList> request(@NonNull Service service) {
        return service.get();
    }

    @VisibleForTesting
    static class CallbackAdapter implements retrofit2.Callback<AnnouncementList> {
        @NonNull private final Callback cb;

        CallbackAdapter(@NonNull Callback cb) {
            this.cb = cb;
        }

        @Override public void onResponse(Call<AnnouncementList> call,
                                         Response<AnnouncementList> response) {
            if (response.isSuccessful()) {
                List<Card> cards = new ArrayList<>();
                AnnouncementList content = response.body();
                if (content != null) {
                    cards.addAll(buildCards(content.items()));
                }
                cb.success(cards);
            } else {
                L.v(response.message());
                cb.error(new IOException(response.message()));
            }
        }

        @Override public void onFailure(Call<AnnouncementList> call, Throwable caught) {
            L.v(caught);
            cb.error(caught);
        }
    }

    private static List<Card> buildCards(@NonNull List<Announcement> announcements) {
        List<Card> cards = new ArrayList<>();
        String country = getGeoIPCountry();
        Date now = new Date();
        for (Announcement announcement : announcements) {
            if (!shouldShow(announcement, country, now)) {
                continue;
            }

            // TODO: add this card!

            L.d("yes!!");

        }
        return cards;
    }

    @VisibleForTesting
    static boolean shouldShow(@Nullable Announcement announcement,
                              @Nullable String country,
                              @NonNull Date date) {
        if (announcement == null
                || !announcement.platforms().contains(PLATFORM_CODE)
                || TextUtils.isEmpty(country)
                || !announcement.countries().contains(country)
                || (announcement.startTime() != null && announcement.startTime().after(date))
                || (announcement.endTime() != null && announcement.endTime().before(date))) {
            return false;
        }
        return true;
    }

    @Nullable private static String getGeoIPCountry() {
        try {
            return GeoIPCookieUnmarshaller.unmarshal(WikipediaApp.getInstance()).country();
        } catch (IllegalArgumentException e) {
            // For our purposes, don't care about malformations in the GeoIP cookie for now.
            return null;
        }
    }
}

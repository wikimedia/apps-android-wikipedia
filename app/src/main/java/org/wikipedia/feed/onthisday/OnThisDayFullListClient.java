package org.wikipedia.feed.onthisday;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;
import org.wikipedia.settings.Prefs;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class OnThisDayFullListClient {
    @Nullable private Call<OnThisDay> call;

    public Call<OnThisDay> request(int month, int date) {
        WikiSite wiki = WikipediaApp.getInstance().getWikiSite();
        String endpoint = String.format(Locale.ROOT, Prefs.getRestbaseUriFormat(), wiki.scheme(),
                wiki.authority());
        Retrofit retrofit = RetrofitFactory.newInstance(endpoint, wiki);
        OnThisDayFullListClient.Service service = retrofit.create(Service.class);
        return service.getAllOtdEvents(month, date);
    }

    @VisibleForTesting
    interface Service {
        @NonNull
        @GET("feed/onthisday/events/{mm}/{dd}")
        Call<OnThisDay> getAllOtdEvents(@Path("mm") int month,
                                        @Path("dd") int day);
    }
}

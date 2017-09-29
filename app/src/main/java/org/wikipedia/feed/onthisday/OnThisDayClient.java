package org.wikipedia.feed.onthisday;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.UtcDate;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class OnThisDayClient implements FeedClient {
    @Nullable private Call<OnThisDay> call;

    @Override
    public void request(@NonNull Context context, @NonNull WikiSite wiki, int age, @NonNull Callback cb) {
        UtcDate today = DateUtil.getUtcRequestDateFor(age);
        String endpoint = String.format(Locale.ROOT, Prefs.getRestbaseUriFormat(), wiki.scheme(),
                wiki.authority());
        Retrofit retrofit = RetrofitFactory.newInstance(endpoint, wiki);
        OnThisDayClient.Service service = retrofit.create(Service.class);
        call = service.getOtdEvents(today.month(), today.date());
        call.enqueue(new CallbackAdapter(cb, today, wiki));
    }

    @Override
    public void cancel() {
        if (call == null) {
            return;
        }
        call.cancel();
        call = null;
    }

    static class CallbackAdapter implements retrofit2.Callback<OnThisDay> {
        @NonNull private final Callback cb;
        private UtcDate today;
        private WikiSite wiki;

        CallbackAdapter(@NonNull Callback cb, UtcDate today, WikiSite wiki) {
            this.cb = cb;
            this.today = today;
            this.wiki = wiki;
        }

        @Override
        public void onResponse(@NonNull Call<OnThisDay> call,
                               @NonNull Response<OnThisDay> response) {
            List<Card> cards = new ArrayList<>();
            OnThisDay onThisDay = response.body();
            int randomIndex = new Random().nextInt(onThisDay.events().size() - 2);
            OnThisDay.Event event = onThisDay.events().get(randomIndex);
            OnThisDayCard card = new OnThisDayCard(onThisDay, event, onThisDay.events().get(randomIndex + 1).year(), today, wiki);
            cards.add(card);
            cb.success(cards);
        }

        @Override
        public void onFailure(@NonNull Call<OnThisDay> call, @NonNull Throwable caught) {
            L.v(caught);
            cb.error(caught);
        }
    }

    @VisibleForTesting
    interface Service {
        @GET("feed/onthisday/events/{mm}/{dd}")
        Call<OnThisDay> getOtdEvents(@Path("mm") String month,
                                     @Path("dd") String day);

    }

    @VisibleForTesting
    @NonNull Call<OnThisDay> request(@NonNull Service service) {
        return service.getOtdEvents("10", "05");
    }

}

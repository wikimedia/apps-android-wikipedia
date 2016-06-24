package org.wikipedia.feed.mostread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Site;
import org.wikipedia.dataclient.retrofit.RbCachedService;
import org.wikipedia.feed.FeedClient;
import org.wikipedia.feed.model.Card;
import org.wikipedia.util.log.L;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class MostReadClient implements FeedClient {
    @NonNull private final RbCachedService<Service> cachedService = new RbCachedService<>(Service.class);
    @Nullable private Call<MostReadArticles> call;

    @Override public void request(@NonNull Context context, @NonNull Site site, int age,
                                  @NonNull Callback cb) {
        cancel();
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        time.add(Calendar.DAY_OF_MONTH, -(age + 1));

        call = request(cachedService.service(site), site, time, cb);
    }

    // Only handles last request made.
    @Override public void cancel() {
        if (call != null) {
            call.cancel();
            call = null;
        }
    }

    @VisibleForTesting Call<MostReadArticles> request(@NonNull Service service, @NonNull Site site,
                                                      @NonNull Calendar date,
                                                      @NonNull Callback cb) {
        int year = date.get(Calendar.YEAR);
        String month = leftPad(date.get(Calendar.MONTH) + 1);
        String day = leftPad(date.get(Calendar.DAY_OF_MONTH));
        @SuppressWarnings("checkstyle:hiddenfield") Call<MostReadArticles> call = service.get(year, month, day);
        call.enqueue(new CallbackAdapter(cb, site));
        return call;
    }

    @NonNull private String leftPad(int x) {
        return StringUtils.leftPad(String.valueOf(x), 2, "0");
    }

    @VisibleForTesting interface Service {
        @GET("page/most-read/{year}/{month}/{day}")
        @NonNull Call<MostReadArticles> get(@Path("year") int year,
                                            @Path("month") @NonNull String month,
                                            @Path("day") @NonNull String day);
    }

    private static class CallbackAdapter implements retrofit2.Callback<MostReadArticles> {
        @NonNull private final Callback cb;
        @NonNull private final Site site;

        CallbackAdapter(@NonNull Callback cb, @NonNull Site site) {
            this.cb = cb;
            this.site = site;
        }

        @Override public void onResponse(Call<MostReadArticles> call,
                                         Response<MostReadArticles> response) {
            if (response.isSuccessful()) {
                MostReadArticles result = response.body();
                List<? extends Card> cards = Collections.singletonList(new MostReadListCard(result, site));
                cb.success(cards);
            } else {
                L.v(response.message());
            }
        }

        @Override public void onFailure(Call<MostReadArticles> call, Throwable t) {
            L.v(t);
        }
    }
}

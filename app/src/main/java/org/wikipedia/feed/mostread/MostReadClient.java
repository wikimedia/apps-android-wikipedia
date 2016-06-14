package org.wikipedia.feed.mostread;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Site;
import org.wikipedia.dataclient.retrofit.RbCachedService;
import org.wikipedia.util.log.L;

import java.util.Calendar;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class MostReadClient {
    public interface Callback {
        void success(@NonNull MostReadArticles articles);
    }

    @NonNull private final RbCachedService<Service> cachedService = new RbCachedService<>(Service.class);

    public void request(@NonNull Site site, @NonNull Callback cb) {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        request(cachedService.service(site), now, cb);
    }

    @VisibleForTesting void request(@NonNull Service service, @NonNull Calendar date,
                                    @NonNull Callback cb) {
        int year = date.get(Calendar.YEAR);
        String month = leftPad(date.get(Calendar.MONTH) + 1);
        String day = leftPad(date.get(Calendar.DAY_OF_MONTH));
        service.get(year, month, day).enqueue(new CallbackAdapter(cb));
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

        CallbackAdapter(@NonNull Callback cb) {
            this.cb = cb;
        }

        @Override public void onResponse(Call<MostReadArticles> call,
                                         Response<MostReadArticles> response) {
            if (response.isSuccessful()) {
                cb.success(response.body());
            } else {
                L.v(response.message());
            }
        }

        @Override public void onFailure(Call<MostReadArticles> call, Throwable t) {
            L.v(t);
        }
    }
}
package org.wikipedia.feed.configure;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.RbCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;

class FeedAvailabilityClient {
    public interface Callback {
        void success(@NonNull Call<FeedAvailability> call, @NonNull FeedAvailability result);
        void failure(@NonNull Call<FeedAvailability> call, @NonNull Throwable caught);
    }

    private final WikiSite wiki = new WikiSite("wikimedia.org");
    @NonNull private final WikiCachedService<Service> cachedService = new RbCachedService<>(Service.class);

    public Call<FeedAvailability> request(@NonNull Callback cb) {
        return request(cachedService.service(wiki), cb);
    }

    @VisibleForTesting Call<FeedAvailability> request(@NonNull Service service, @NonNull final Callback cb) {
        Call<FeedAvailability> call = service.get();
        call.enqueue(new retrofit2.Callback<FeedAvailability>() {
            @Override
            public void onResponse(@NonNull Call<FeedAvailability> call, @NonNull Response<FeedAvailability> response) {
                if (response.body() != null) {
                    // noinspection ConstantConditions
                    cb.success(call, response.body());
                } else {
                    // noinspection ConstantConditions
                    cb.failure(call, new RuntimeException("Incorrect response format"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<FeedAvailability> call, @NonNull Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    @VisibleForTesting interface Service {
        @NonNull
        @GET("feed/availability")
        Call<FeedAvailability> get();
    }

    public class FeedAvailability {
        @SuppressWarnings("unused") @SerializedName("todays_featured_article") private List<String> featuredArticle;
        @SuppressWarnings("unused") @SerializedName("most_read") private List<String> mostRead;
        @SuppressWarnings("unused") @SerializedName("picture_of_the_day") private List<String> featuredPicture;
        @SuppressWarnings("unused") @SerializedName("in_the_news") private List<String> news;
        @SuppressWarnings("unused") @SerializedName("on_this_day") private List<String> onThisDay;

        @NonNull public List<String> featuredArticle() {
            return featuredArticle != null ? featuredArticle : Collections.emptyList();
        }

        @NonNull public List<String> mostRead() {
            return mostRead != null ? mostRead : Collections.emptyList();
        }

        @NonNull public List<String> featuredPicture() {
            return featuredPicture != null ? featuredPicture : Collections.emptyList();
        }

        @NonNull public List<String> news() {
            return news != null ? news : Collections.emptyList();
        }

        @NonNull public List<String> onThisDay() {
            return onThisDay != null ? onThisDay : Collections.emptyList();
        }
    }
}

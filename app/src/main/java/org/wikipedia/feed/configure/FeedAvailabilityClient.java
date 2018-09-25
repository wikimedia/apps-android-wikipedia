package org.wikipedia.feed.configure;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.RestService;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class FeedAvailabilityClient {
    public interface Callback {
        void success(@NonNull Call<FeedAvailability> call, @NonNull FeedAvailability result);
        void failure(@NonNull Call<FeedAvailability> call, @NonNull Throwable caught);
    }

    private final WikiSite wiki = new WikiSite("wikimedia.org");

    public Call<FeedAvailability> request(@NonNull Callback cb) {
        return request(ServiceFactory.getRest(wiki), cb);
    }

    @VisibleForTesting Call<FeedAvailability> request(@NonNull RestService service, @NonNull final Callback cb) {
        Call<FeedAvailability> call = service.getFeedAvailability();
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

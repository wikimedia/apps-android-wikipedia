package org.wikipedia.feed.configure;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class FeedAvailability {
    @SerializedName("todays_featured_article") private List<String> featuredArticle;
    @SerializedName("most_read") private List<String> mostRead;
    @SerializedName("picture_of_the_day") private List<String> featuredPicture;
    @SerializedName("in_the_news") private List<String> news;
    @SerializedName("on_this_day") private List<String> onThisDay;

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

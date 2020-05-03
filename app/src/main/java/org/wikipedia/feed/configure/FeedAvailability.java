package org.wikipedia.feed.configure;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.collections4.ListUtils;

import java.util.List;

@SuppressWarnings("unused")
public class FeedAvailability {
    @SerializedName("todays_featured_article") private List<String> featuredArticle;
    @SerializedName("most_read") private List<String> mostRead;
    @SerializedName("picture_of_the_day") private List<String> featuredPicture;
    @SerializedName("in_the_news") private List<String> news;
    @SerializedName("on_this_day") private List<String> onThisDay;

    @NonNull public List<String> featuredArticle() {
        return ListUtils.emptyIfNull(featuredArticle);
    }

    @NonNull public List<String> mostRead() {
        return ListUtils.emptyIfNull(mostRead);
    }

    @NonNull public List<String> featuredPicture() {
        return ListUtils.emptyIfNull(featuredPicture);
    }

    @NonNull public List<String> news() {
        return ListUtils.emptyIfNull(news);
    }

    @NonNull public List<String> onThisDay() {
        return ListUtils.emptyIfNull(onThisDay);
    }
}

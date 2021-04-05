package org.wikipedia.feed.announcement;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.json.annotations.Required;
import org.wikipedia.util.DateUtil;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.defaultString;

@SuppressWarnings("unused")
public class Announcement {
    public static final String SURVEY = "survey";
    public static final String FUNDRAISING = "fundraising";
    public static final String PLACEMENT_FEED = "feed";
    public static final String PLACEMENT_ARTICLE = "article";

    @SuppressWarnings("NullableProblems") @Required @NonNull private String id;
    @SuppressWarnings("NullableProblems") @Required @NonNull private String type;
    @SuppressWarnings("NullableProblems") @SerializedName("start_time") @Required @NonNull private String startTime;
    @SuppressWarnings("NullableProblems") @SerializedName("end_time") @Required @NonNull private String endTime;
    @NonNull private List<String> platforms = Collections.emptyList();
    @NonNull private List<String> countries = Collections.emptyList();
    @SerializedName("caption_HTML") @Nullable private String footerCaption;
    @SerializedName("image_url") @Nullable private String imageUrl;
    @SerializedName("image_height") @Nullable private String imageHeight;
    @SerializedName("logged_in") @Nullable private Boolean loggedIn;
    @SerializedName("reading_list_sync_enabled") @Nullable private Boolean readingListSyncEnabled;
    @Nullable private Boolean beta;
    @Nullable private Boolean border;
    @Nullable private String placement;
    @SerializedName("min_version") @Nullable private String minVersion;
    @SerializedName("max_version") @Nullable private String maxVersion;

    @SuppressWarnings("NullableProblems") @Required @NonNull private String text;
    @Nullable private Action action;
    @SerializedName("negative_text") @Nullable private String negativeText;

    public Announcement() { }

    public Announcement(@NonNull String id, @NonNull String text, @NonNull String imageUrl,
                        @NonNull Action action, @NonNull String negativeText) {
        this.id = id;
        this.text = text;
        this.imageUrl = imageUrl;
        this.action = action;
        this.negativeText = negativeText;
    }

    @NonNull
    public String id() {
        return id;
    }

    @NonNull
    public String type() {
        return type;
    }

    @Nullable
    public Date startTime() {
        return DateUtil.iso8601DateParse(startTime);
    }

    @Nullable Date endTime() {
        return DateUtil.iso8601DateParse(endTime);
    }

    @NonNull List<String> platforms() {
        return platforms;
    }

    @NonNull List<String> countries() {
        return countries;
    }

    @NonNull String text() {
        return text;
    }

    boolean hasAction() {
        return action != null;
    }

    @NonNull String actionTitle() {
        return action != null ? action.title() : "";
    }

    @NonNull String actionUrl() {
        return action != null ? action.url() : "";
    }

    boolean hasFooterCaption() {
        return !TextUtils.isEmpty(footerCaption);
    }

    @NonNull String footerCaption() {
        return defaultString(footerCaption);
    }

    boolean hasImageUrl() {
        return !TextUtils.isEmpty(imageUrl);
    }

    @NonNull String imageUrl() {
        return defaultString(imageUrl);
    }

    @NonNull String imageHeight() {
        return defaultString(imageHeight);
    }

    @Nullable String negativeText() {
        return negativeText;
    }

    @Nullable Boolean loggedIn() {
        return loggedIn;
    }

    @Nullable Boolean readingListSyncEnabled() {
        return readingListSyncEnabled;
    }

    @Nullable Boolean beta() {
        return beta;
    }

    @NonNull
    public String placement() {
        return defaultString(placement, PLACEMENT_FEED);
    }

    boolean hasBorder() {
        return border != null && border;
    }

    @Nullable String minVersion() {
        return minVersion;
    }

    @Nullable String maxVersion() {
        return maxVersion;
    }

    public static class Action {
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String title;
        @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String url;

        public Action(@NonNull String title, @NonNull String url) {
            this.title = title;
            this.url = url;
        }

        @NonNull String title() {
            return title;
        }

        @NonNull String url() {
            return url;
        }
    }
}

package org.wikipedia.feed.announcement;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.json.annotations.Required;
import org.wikipedia.model.BaseModel;
import org.wikipedia.util.DateUtil;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class Announcement extends BaseModel {
    public static final String SURVEY = "survey";
    public static final String FUNDRAISING = "fundraising";

    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String id;
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String type;
    @SuppressWarnings("unused,NullableProblems") @SerializedName("start_time") @Required @NonNull private String startTime;
    @SuppressWarnings("unused,NullableProblems") @SerializedName("end_time") @Required @NonNull private String endTime;
    @SuppressWarnings("unused") @NonNull private List<String> platforms = Collections.emptyList();
    @SuppressWarnings("unused") @NonNull private List<String> countries = Collections.emptyList();
    @SuppressWarnings("unused") @SerializedName("caption_HTML") @Nullable private String footerCaption;
    @SuppressWarnings("unused") @SerializedName("image") @Nullable private String imageUrl;

    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String text;
    @SuppressWarnings("unused") @Nullable private Action action;

    public Announcement() { }

    public Announcement(@NonNull String id, @NonNull String text, @NonNull String imageUrl, @NonNull Action action) {
        this.id = id;
        this.text = text;
        this.imageUrl = imageUrl;
        this.action = action;
    }

    @NonNull String id() {
        return id;
    }

    @NonNull String type() {
        return type;
    }

    @Nullable Date startTime() {
        try {
            return DateUtil.getIso8601DateFormat().parse(startTime);
        } catch (ParseException e) {
            return null;
        }
    }

    @Nullable Date endTime() {
        try {
            return DateUtil.getIso8601DateFormat().parse(endTime);
        } catch (ParseException e) {
            return null;
        }
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

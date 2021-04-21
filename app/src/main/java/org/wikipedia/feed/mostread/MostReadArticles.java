package org.wikipedia.feed.mostread;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.page.PageSummary;

import java.util.Date;
import java.util.List;

@SuppressWarnings("unused")
public final class MostReadArticles extends PageSummary {
    private int views;
    private int rank;
    @SerializedName("view_history") private List<ViewHistory> viewHistory;

    public int getViews() {
        return views;
    }

    @Nullable
    public List<ViewHistory> getViewHistory() {
        return viewHistory;
    }

    public class ViewHistory {
        private Date date;
        private float views;

        @NonNull
        public Date getDate() {
            return date;
        }

        public float getViews() {
            return views;
        }
    }
}

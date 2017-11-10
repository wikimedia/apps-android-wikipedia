package org.wikipedia.dataclient.restbase.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.dataclient.restbase.RbServiceError;
import org.wikipedia.json.annotations.Required;

/**
 * A standardized page summary object constructed by RESTBase, used for link previews and as the
 * base class for various feed content (see the FeedPageSummary class).
 *
 * N.B.: The "title" field here sent by RESTBase is the *normalized* page title.  However, in the
 * FeedPageSummary subclass, "title" becomes the un-normalized, raw title, and the normalized title
 * is sent as "normalizedtitle".
 */
public class RbPageSummary implements PageSummary {
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String title;
    @SuppressWarnings("unused") @Nullable private String normalizedtitle;
    @SuppressWarnings("unused,NullableProblems") @NonNull private String displaytitle;
    @SuppressWarnings("unused") @Nullable private String extract;
    @SuppressWarnings("unused") @Nullable private String description;
    @SuppressWarnings("unused") @Nullable private Thumbnail thumbnail;
    @SuppressWarnings("unused") @Nullable @SerializedName("originalimage") private Thumbnail originalImage;

    @Override
    public boolean hasError() {
        // If we have a page summary object, RESTBase hasn't returned an error
        return false;
    }

    @Override @Nullable
    public RbServiceError getError() {
        return null;
    }

    @Override @NonNull
    public String getTitle() {
        return title;
    }

    @Override @NonNull
    public String getDisplayTitle() {
        return displaytitle;
    }

    @Override @Nullable
    public String getExtract() {
        return extract;
    }

    @Override @Nullable
    public String getThumbnailUrl() {
        return thumbnail == null ? null : thumbnail.getUrl();
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @NonNull
    public String getNormalizedTitle() {
        return normalizedtitle == null ? title : normalizedtitle;
    }

    @Nullable
    public String getOriginalImageUrl() {
        return originalImage == null ? null : originalImage.getUrl();
    }

    /**
     * For the thumbnail URL of the page
     */
    private static class Thumbnail {
        @SuppressWarnings("unused") private String source;

        public String getUrl() {
            return source;
        }
    }


    public void setDescription(@Nullable String description) {
        this.description = description;
    }

}

package org.wikipedia.feed.image;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.feed.model.Thumbnail;
import org.wikipedia.gallery.ArtistInfo;
import org.wikipedia.gallery.ImageLicense;
import org.wikipedia.json.annotations.Required;

public final class FeaturedImage {
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String title;
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private Thumbnail thumbnail;
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private Thumbnail image;
    @SuppressWarnings("unused") @Nullable private Description description;
    @SuppressWarnings("unused,NullableProblems") @NonNull @SerializedName("file_page") private String filePage;
    @SuppressWarnings("unused") @Nullable private ArtistInfo artist;
    @SuppressWarnings("unused,NullableProblems") @NonNull private ImageLicense license;

    @NonNull
    public String title() {
        return title;
    }

    @NonNull
    public Thumbnail thumbnail() {
        return thumbnail;
    }

    @NonNull
    public Thumbnail image() {
        return image;
    }

    @Nullable
    public String description() {
        return description == null ? null : description.text;
    }

    @Nullable
    public String descriptionLang() {
        return description == null ? null : description.lang;
    }

    @NonNull
    public String filePage() {
        return filePage;
    }

    @Nullable
    public ArtistInfo artist() {
        return artist;
    }

    @NonNull
    public ImageLicense license() {
        return license;
    }


    /**
     * An object containing a description of the featured image and a language code for that description.
     *
     * The content service gets all available translations of the featured image description and
     * returns the translation for the request wiki language, if available.  Otherwise it defaults
     * to providing the English translation.
     */
    private static class Description {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String text;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String lang;

        public String text() {
            return text;
        }

        public String lang() {
            return lang;
        }
    }
}

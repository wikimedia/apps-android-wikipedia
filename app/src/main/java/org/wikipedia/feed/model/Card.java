package org.wikipedia.feed.model;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class Card {
    @NonNull public String title() {
        return "";
    }

    @Nullable public String subtitle() {
        return null;
    }

    @Nullable public Uri image() {
        return null;
    }

    @Nullable public String extract() {
        return null;
    }

    @NonNull public abstract CardType type();

    public void onDismiss() {
    }

    public void onRestore() {
    }

    public String getHideKey() {
        return Long.toString(type().code() + dismissHashCode());
    }

    protected int dismissHashCode() {
        return hashCode();
    }

    @Override
    public int hashCode() {
        return 31 * type().code() + title().hashCode();
    }
}

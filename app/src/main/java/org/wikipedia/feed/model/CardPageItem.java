package org.wikipedia.feed.model;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.JsonAdapter;

import org.wikipedia.page.Namespace;
import org.wikipedia.page.NamespaceTypeAdapter;
import org.wikipedia.util.StringUtil;

public final class CardPageItem {
    @SuppressWarnings("unused,NullableProblems") @NonNull private String title;
    @SuppressWarnings("unused") @Nullable private Thumbnail thumbnail;
    @SuppressWarnings("unused") @Nullable private String description;
    @SuppressWarnings("unused") @Nullable private String extract;
    @SuppressWarnings("unused") @Nullable @JsonAdapter(NamespaceTypeAdapter.class) private Namespace namespace;

    @NonNull
    public String title() {
        return title;
    }

    @Nullable
    public String description() {
        return description;
    }

    @Nullable
    public String extract() {
        // todo: the service should strip IPA.
        return extract == null ? null : StringUtil.sanitizeText(extract);
    }

    @Nullable
    public Namespace namespace() {
        return namespace;
    }

    @Nullable
    public Uri thumbnail() {
        return thumbnail != null ? thumbnail.source() : null;
    }
}

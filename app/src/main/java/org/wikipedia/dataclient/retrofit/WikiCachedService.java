package org.wikipedia.dataclient.retrofit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;

import retrofit2.Retrofit;

public abstract class WikiCachedService<T> extends CachedService<T> {
    @Nullable private WikiSite wiki;

    public WikiCachedService(@NonNull Class<T> clazz) {
        super(clazz);
    }

    @Nullable public WikiSite wiki() {
        return wiki;
    }

    @NonNull public T service(@NonNull WikiSite wiki) {
        update(wiki);
        //noinspection ConstantConditions
        return super.service();
    }

    @NonNull public Retrofit retrofit(@NonNull WikiSite wiki) {
        update(wiki);
        //noinspection ConstantConditions
        return super.retrofit();
    }

    private void update(@NonNull WikiSite wiki) {
        if (outdated(wiki)) {
            this.wiki = wiki;
            super.update();
        }
    }

    private boolean outdated(@NonNull WikiSite wiki) {
        return super.outdated() || !wiki.equals(this.wiki);
    }
}

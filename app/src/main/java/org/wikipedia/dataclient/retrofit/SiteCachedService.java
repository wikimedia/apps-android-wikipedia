package org.wikipedia.dataclient.retrofit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.Site;

import retrofit2.Retrofit;

public abstract class SiteCachedService<T> extends CachedService<T> {
    @Nullable private Site site;

    public SiteCachedService(@NonNull Class<T> clazz) {
        super(clazz);
    }

    @NonNull public T service(@NonNull Site site) {
        update(site);
        //noinspection ConstantConditions
        return super.service();
    }

    @NonNull public Retrofit retrofit(@NonNull Site site) {
        update(site);
        //noinspection ConstantConditions
        return super.retrofit();
    }

    @NonNull @Override protected Retrofit create() {
        //noinspection ConstantConditions
        return create(site);
    }

    @NonNull protected abstract Retrofit create(@NonNull Site site);

    private void update(@NonNull Site site) {
        if (outdated(site)) {
            this.site = site;
            super.update();
        }
    }

    private boolean outdated(@NonNull Site site) {
        return super.outdated() || !site.equals(this.site);
    }
}
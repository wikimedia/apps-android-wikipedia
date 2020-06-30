package org.wikipedia.dataclient.okhttp;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;

import java.io.InputStream;

/**
 * NOTE: do not convert this class to Kotlin yet. The Glide annotation does not support it.
 */
@GlideModule
public class OkHttpGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, Glide glide, @NonNull Registry registry) {
        glide.getRegistry().replace(GlideUrl.class, InputStream.class,
                new OkHttpUrlLoader.Factory(OkHttpConnectionFactory.getClient()));
    }
}

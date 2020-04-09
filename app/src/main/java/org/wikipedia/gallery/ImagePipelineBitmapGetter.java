package org.wikipedia.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.wikipedia.WikipediaApp;

public abstract class ImagePipelineBitmapGetter {
    private String imageUrl;

    public ImagePipelineBitmapGetter(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public abstract void onSuccess(@Nullable Bitmap bitmap);

    public void onError(Throwable t) {
        Toast.makeText(WikipediaApp.getInstance(), t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }

    public void get(@NonNull Context context) {
        Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        onSuccess(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }
}

package org.wikipedia.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import org.wikipedia.views.FaceAndColorDetectImageView;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class WhiteBackgroundTransformation extends BitmapTransformation {
    private static final String ID = "org.wikipedia.util.WhiteBackgroundTransformation";
    private static final byte[] ID_BYTES = ID.getBytes(StandardCharsets.UTF_8);

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        if (toTransform.hasAlpha()) {
            Bitmap result = pool.get(toTransform.getWidth(), toTransform.getHeight(),
                    toTransform.getConfig() != null ? toTransform.getConfig() : Bitmap.Config.RGB_565);
            FaceAndColorDetectImageView.applyMatrixWithBackground(toTransform, result, new Matrix());
            return result;
        } else {
            return toTransform;
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WhiteBackgroundTransformation;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
    }
}

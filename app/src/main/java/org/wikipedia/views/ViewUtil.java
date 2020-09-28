package org.wikipedia.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.WhiteBackgroundTransformation;

import java.util.Locale;

import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;

@SuppressWarnings("checkstyle:magicnumber")
public final class ViewUtil {
    private static final RoundedCorners ROUNDED_CORNERS = new RoundedCorners(DimenUtil.roundedDpToPx(15));
    private static final MultiTransformation<Bitmap> CENTER_CROP_LARGE_ROUNDED_CORNERS = new MultiTransformation<>(new CenterCrop(), ROUNDED_CORNERS);
    private static final MultiTransformation<Bitmap> CENTER_CROP_ROUNDED_CORNERS = new MultiTransformation<>(new CenterCrop(), new RoundedCorners(DimenUtil.roundedDpToPx(2)));

    public static RoundedCorners getRoundedCorners() {
        return ROUNDED_CORNERS;
    }

    public static Bitmap cachedBitmap;

    public static MultiTransformation<Bitmap> getCenterCropLargeRoundedCorners() {
        return CENTER_CROP_LARGE_ROUNDED_CORNERS;
    }

    public static void loadImageWithRoundedCorners(@NonNull ImageView view, @Nullable String url) {
        loadImage(view, url, true, false, false);
    }

    public static void loadImageWithRoundedCorners(@NonNull ImageView view, @Nullable String url, boolean largeRoundedSize) {
        loadImage(view, url, true, largeRoundedSize, false);
    }

    public static void loadImage(@NonNull ImageView view, @Nullable String url) {
        loadImage(view, url, false, false, false);
    }

    public static void loadImage(@NonNull ImageView view, @Nullable String url, boolean roundedCorners, boolean largeRoundedSize, boolean force) {
        Drawable placeholder = getPlaceholderDrawable(view.getContext());
        RequestBuilder<Drawable> builder = Glide.with(view)
                .load((isImageDownloadEnabled() || force) && !TextUtils.isEmpty(url) ? Uri.parse(url) : null)
                .placeholder(placeholder)
                .downsample(DownsampleStrategy.CENTER_INSIDE)
                .error(placeholder);
        if (roundedCorners) {
            builder = builder.transform(largeRoundedSize ? CENTER_CROP_LARGE_ROUNDED_CORNERS : CENTER_CROP_ROUNDED_CORNERS);
        }
        builder.into(view);
    }

    public static void loadImageWithWhiteBackground(@NonNull ImageView view, @Nullable String url) {
        Drawable placeholder = getPlaceholderDrawable(view.getContext());
        Glide.with(view)
                .load(!TextUtils.isEmpty(url) ? Uri.parse(url) : null)
                .placeholder(placeholder)
                .error(placeholder)
                .downsample(DownsampleStrategy.CENTER_INSIDE)
                .transform(new WhiteBackgroundTransformation())
                .into(view);
    }

    static Drawable getPlaceholderDrawable(@NonNull Context context) {
        return new ColorDrawable(ResourceUtil.getThemedColor(context, R.attr.material_theme_border_color));
    }

    public static void setCloseButtonInActionMode(@NonNull Context context, @NonNull android.view.ActionMode actionMode) {
        View view = View.inflate(context, R.layout.view_action_mode_close_button, null);
        actionMode.setCustomView(view);
        ImageView closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> actionMode.finish());
    }

    @NonNull
    public static Bitmap getBitmapFromView(@NonNull View view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        view.draw(canvas);
        return returnedBitmap;
    }

    public static void setCachedBitmap(@Nullable Bitmap bitmap) {
        cachedBitmap = bitmap;
    }

    public static void setViewCachedBitmap(@NonNull ImageView view) {
        if (cachedBitmap != null) {
            view.setImageBitmap(cachedBitmap);
        }
    }

    public static void formatLangButton(@NonNull TextView langButton, @NonNull String langCode,
                                        int langButtonTextSizeSmaller, int langButtonTextSizeLarger) {
        final int langCodeStandardLength = 3;
        final int langButtonTextMaxLength = 7;

        if (langCode.length() > langCodeStandardLength) {
            langButton.setTextSize(langButtonTextSizeSmaller);
            if (langCode.length() > langButtonTextMaxLength) {
                langButton.setText(langCode.substring(0, langButtonTextMaxLength).toUpperCase(Locale.ENGLISH));
            }
            return;
        }
        langButton.setTextSize(langButtonTextSizeLarger);
    }

    public static int adjustImagePlaceholderHeight(Float containerWidth, Float thumbWidth, Float thumbHeight) {
        return (int) ((float) Constants.PREFERRED_GALLERY_IMAGE_SIZE / thumbWidth * thumbHeight * containerWidth / (float) Constants.PREFERRED_GALLERY_IMAGE_SIZE);
    }

    private ViewUtil() {
    }
}

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
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;

import java.util.Locale;

import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;

public final class ViewUtil {
    private static Drawable PLACEHOLDER_DRAWABLE = null;
    private static MultiTransformation<Bitmap> CENTER_CROP_ROUNDED_CORNERS = new MultiTransformation<>(new CenterCrop(), new RoundedCorners(DimenUtil.roundedDpToPx(2)));

    public static void loadImageWithRoundedCorners(@NonNull ImageView view, @Nullable String url) {
        loadImage(view, url, true, false);
    }

    public static void loadImage(@NonNull ImageView view, @Nullable String url) {
        loadImage(view, url, false, false);
    }

    public static void loadImage(@NonNull ImageView view, @Nullable String url, boolean roundedCorners, boolean force) {
        RequestBuilder<Drawable> builder = Glide.with(view)
                .load((isImageDownloadEnabled() || force) && !TextUtils.isEmpty(url) ? Uri.parse(url) : null)
                .placeholder(getPlaceholderDrawable(view.getContext()))
                .error(getPlaceholderDrawable(view.getContext()));
        if (roundedCorners) {
            builder = builder.transform(CENTER_CROP_ROUNDED_CORNERS);
        }
        builder.into(view);
    }

    static Drawable getPlaceholderDrawable(@NonNull Context context) {
        if (PLACEHOLDER_DRAWABLE == null) {
            PLACEHOLDER_DRAWABLE = new ColorDrawable(ResourceUtil.getThemedColor(context, R.attr.material_theme_border_color));
        }
        return PLACEHOLDER_DRAWABLE;
    }

    public static void clearPlaceholderDrawable() {
        PLACEHOLDER_DRAWABLE = null;
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

    private ViewUtil() {
    }
}

package org.wikipedia.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.WhiteBackgroundTransformation;

import java.util.Locale;

import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;

public final class ViewUtil {
    private static MultiTransformation<Bitmap> CENTER_CROP_ROUNDED_CORNERS = new MultiTransformation<>(new CenterCrop(), new RoundedCorners(DimenUtil.roundedDpToPx(2)));

    public static void loadImageWithRoundedCorners(@NonNull ImageView view, @Nullable String url) {
        loadImage(view, url, true, false);
    }

    public static void loadImage(@NonNull ImageView view, @Nullable String url) {
        loadImage(view, url, false, false);
    }

    public static void loadImage(@NonNull ImageView view, @Nullable String url, boolean roundedCorners, boolean force) {
        Drawable placeholder = getPlaceholderDrawable(view.getContext());
        RequestBuilder<Drawable> builder = Glide.with(view)
                .load((isImageDownloadEnabled() || force) && !TextUtils.isEmpty(url) ? Uri.parse(url) : null)
                .placeholder(placeholder)
                .downsample(DownsampleStrategy.CENTER_INSIDE)
                .error(placeholder);
        if (roundedCorners) {
            builder = builder.transform(CENTER_CROP_ROUNDED_CORNERS);
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

    public static void setActionBarElevation(@NonNull ViewGroup scrollableView, @NonNull AppCompatActivity activity) {
        if (activity.getSupportActionBar() == null) {
            return;
        }
        if (scrollableView instanceof ScrollView && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ((ScrollView) scrollableView).setOnScrollChangeListener((View.OnScrollChangeListener) (view, i, scrollY, i2, i3) ->
                    enableElevation(activity.getSupportActionBar(), scrollY != 0));
        } else if (scrollableView instanceof NestedScrollView && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ((NestedScrollView) scrollableView).setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (view, i, scrollY, i2, i3) ->
                    enableElevation(activity.getSupportActionBar(), scrollY != 0));
        } else if (scrollableView instanceof RecyclerView) {
            ((RecyclerView) scrollableView).addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    enableElevation(activity.getSupportActionBar(), ((RecyclerView) scrollableView).computeVerticalScrollOffset() != 0);
                }
            });
        } else {
            enableElevation(activity.getSupportActionBar(), true);
        }
    }

    public static void enableElevation(@Nullable ActionBar actionBar, boolean enabled) {
        if (actionBar == null) {
            return;
        }
        actionBar.setElevation(enabled ? DimenUtil.dpToPx(DimenUtil.getDimension(R.dimen.toolbar_default_elevation)) : 0);
    }

    private ViewUtil() {
    }
}

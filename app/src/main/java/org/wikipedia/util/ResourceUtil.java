package org.wikipedia.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.AnyRes;
import android.support.annotation.ArrayRes;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.TypedValue;
import android.view.MenuItem;

public final class ResourceUtil {
    // See Resources.getIdentifier().
    private static final int NO_ID = 0;

    public static int[] getIdArray(Context context, @ArrayRes int id) {
        return getIdArray(context.getResources(), id);
    }

    public static int[] getIdArray(Resources resources, @ArrayRes int id) {
        TypedArray typedArray = resources.obtainTypedArray(id);
        int[] ids = new int[typedArray.length()];
        for (int i = 0; i < typedArray.length(); ++i) {
            @IdRes int itemId = typedArray.getResourceId(i, NO_ID);
            ids[i] = itemId;
            checkId(itemId);
        }
        typedArray.recycle();
        return ids;
    }

    @NonNull
    public static Bitmap bitmapFromVectorDrawable(@NonNull Context context, @DrawableRes int id, @ColorRes Integer tintColor) {
        Drawable vectorDrawable = VectorDrawableCompat.create(context.getResources(), id, null);
        int width = vectorDrawable.getIntrinsicWidth();
        int height = vectorDrawable.getIntrinsicHeight();
        vectorDrawable.setBounds(0, 0, width, height);
        if (tintColor != null) {
            DrawableCompat.setTint(vectorDrawable, ContextCompat.getColor(context, tintColor));
        }
        Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        vectorDrawable.draw(canvas);
        return bm;
    }

    @Nullable public static TypedValue getThemedAttribute(@NonNull Context context, @AttrRes int id) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(id, typedValue, true)) {
            return typedValue;
        }
        return null;
    }

    /**
     * Resolves the resource ID of a theme-dependent attribute (for example, a color value
     * that changes based on the selected theme)
     * @param context The context whose theme contains the attribute.
     * @param id Theme-dependent attribute ID to be resolved.
     * @return The actual resource ID of the requested theme-dependent attribute.
     */
    @AnyRes public static int getThemedAttributeId(@NonNull Context context, @AttrRes int id) {
        TypedValue typedValue = getThemedAttribute(context, id);
        if (typedValue == null) {
            throw new IllegalArgumentException("Attribute not found; ID=" + id);
        }
        return typedValue.resourceId;
    }

    @ColorInt public static int getThemedColor(@NonNull Context context, @AttrRes int id) {
        TypedValue typedValue = getThemedAttribute(context, id);
        if (typedValue == null) {
            throw new IllegalArgumentException("Attribute not found; ID=" + id);
        }
        return typedValue.data;
    }

    public static Uri uri(@NonNull Context context, @AnyRes int id) throws Resources.NotFoundException {
        Resources res = context.getResources();
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(res.getResourcePackageName(id))
                .appendPath(res.getResourceTypeName(id))
                .appendPath(res.getResourceEntryName(id))
                .build();
    }

    public static void setMenuItemTint(@NonNull MenuItem item, @ColorInt int color) {
        Drawable icon = item.getIcon();
        Drawable wrapped = DrawableCompat.wrap(icon);
        icon.mutate();
        DrawableCompat.setTint(wrapped, color);
        item.setIcon(wrapped);
    }

    private static void checkId(@IdRes int id) {
        if (!isIdValid(id)) {
            throw new RuntimeException("id is invalid");
        }
    }

    private static boolean isIdValid(@IdRes int id) {
        return id != NO_ID;
    }

    private ResourceUtil() { }
}

package org.wikipedia.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.support.annotation.AnyRes;
import android.support.annotation.ArrayRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.util.TypedValue;

public final class ResourceUtil {
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

    /**
     * Resolves the resource ID of a theme-dependent attribute (for example, a color value
     * that changes based on the selected theme)
     * @param context The context whose theme contains the attribute.
     * @param id Theme-dependent attribute ID to be resolved.
     * @return The actual resource ID of the requested theme-dependent attribute.
     */
    @AnyRes public static int getThemedAttributeId(Context context, int id) {
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(id, tv, true);
        return tv.resourceId;
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
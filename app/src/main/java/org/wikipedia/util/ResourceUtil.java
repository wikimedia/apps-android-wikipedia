package org.wikipedia.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.LocaleList;
import android.support.annotation.AnyRes;
import android.support.annotation.ArrayRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.util.TypedValue;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

import java.util.Locale;

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

    @DrawableRes @SuppressWarnings("checkstyle:magicnumber") public static int getTabListIcon(int numTabs) {
        switch (Math.max(0, numTabs)) {
            case 0: return R.drawable.ic_tab_list_white_24dp;
            case 1: return R.drawable.ic_tab_list_1;
            case 2: return R.drawable.ic_tab_list_2;
            case 3: return R.drawable.ic_tab_list_3;
            case 4: return R.drawable.ic_tab_list_4;
            case 5: return R.drawable.ic_tab_list_5;
            case 6: return R.drawable.ic_tab_list_6;
            case 7: return R.drawable.ic_tab_list_7;
            case 8: return R.drawable.ic_tab_list_8;
            case 9: return R.drawable.ic_tab_list_9;
            default: return R.drawable.ic_tab_list_9_plus;
        }
    }

    @NonNull
    public static Bitmap bitmapFromVectorDrawable(@NonNull Context context, @DrawableRes int id) {
        Drawable vectorDrawable = VectorDrawableCompat.create(context.getResources(), id, null);
        int width = vectorDrawable.getIntrinsicWidth();
        int height = vectorDrawable.getIntrinsicHeight();
        vectorDrawable.setBounds(0, 0, width, height);
        Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        vectorDrawable.draw(canvas);
        return bm;
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

    public static void setLocale(@NonNull Context ctx, @NonNull String lang) {
        // todo: this conversion is performed elsewhere in the app but is probably buggy
        Locale locale = new Locale(lang);
        String sysLang = WikipediaApp.getInstance().getSystemLanguageCode();
        Configuration cfg = new Configuration(ctx.getResources().getConfiguration());
        if (lang.equals(sysLang)) {
            setLocale(ctx, cfg, locale);
        } else {
            setLocale(ctx, cfg, locale, new Locale(sysLang));
        }
    }

    public static void setLocale(@NonNull Context ctx, @NonNull Configuration cfg,
                                 @NonNull Locale... locales) {
        Locale.setDefault(locales[0]);

        setConfigLocale(cfg, locales);
        ctx.getResources().updateConfiguration(cfg, ctx.getResources().getDisplayMetrics());
    }

    private static void setConfigLocale(@NonNull Configuration config, @NonNull Locale... locales) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(locales));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locales[0]);
            config.setLayoutDirection(locales[0]);
        } else {
            //noinspection deprecation
            config.locale = locales[0];
        }
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

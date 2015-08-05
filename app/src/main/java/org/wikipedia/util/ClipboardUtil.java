package org.wikipedia.util;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;

public final class ClipboardUtil {
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void setPlainText(@Nullable Context context,
                                    @Nullable CharSequence label,
                                    @Nullable CharSequence text) {
        if (ApiUtil.hasHoneyComb()) {
            ClipData clip = ClipData.newPlainText(label, text);
            getManager(context).setPrimaryClip(clip);
        } else {
            getManagerGb(context).setText(text);
        }
    }

    private static ClipboardManager getManager(Context context) {
        return (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @SuppressWarnings("deprecation")
    private static android.text.ClipboardManager getManagerGb(Context context) {
        return (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    private ClipboardUtil() { }
}
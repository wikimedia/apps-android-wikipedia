package org.wikipedia.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.annotation.Nullable;

public final class ClipboardUtil {
    public static void setPlainText(@Nullable Context context,
                                    @Nullable CharSequence label,
                                    @Nullable CharSequence text) {
        ClipData clip = ClipData.newPlainText(label, text);
        getManager(context).setPrimaryClip(clip);
    }

    private static ClipboardManager getManager(Context context) {
        return (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    private ClipboardUtil() { }
}

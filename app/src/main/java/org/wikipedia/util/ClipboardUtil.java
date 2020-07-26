package org.wikipedia.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public final class ClipboardUtil {
    public static void setPlainText(@Nullable Context context,
                                    @Nullable CharSequence label,
                                    @Nullable CharSequence text) {
        ClipData clip = ClipData.newPlainText(label, text);
        ContextCompat.getSystemService(context, ClipboardManager.class).setPrimaryClip(clip);
    }

    private ClipboardUtil() { }
}

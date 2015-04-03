package org.wikipedia.util;

import android.content.Context;

import org.wikipedia.R;

/**
 * A collection of localization `related methods.
 */
public class L10nUtils {
    public static boolean canLangUseImageForWikipediaWordmark(Context context) {
        return "<big>W</big>IKIPEDI<big>A</big>".equals(context.getString(R.string.wp_stylized));
    }
}

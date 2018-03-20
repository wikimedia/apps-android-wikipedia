package org.wikipedia.settings;

import android.content.Context;
import android.util.AttributeSet;

import org.wikipedia.R;

public class PreferenceMultiLineWithExternalLink extends PreferenceMultiLine {

    public PreferenceMultiLineWithExternalLink(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        init();
    }

    public PreferenceMultiLineWithExternalLink(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    public PreferenceMultiLineWithExternalLink(Context ctx) {
        super(ctx);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_multiline_with_external_link);
    }
}

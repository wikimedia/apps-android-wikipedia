package org.wikipedia.settings;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class CheckBoxPreferenceMultiLine extends CheckBoxPreference {

    public CheckBoxPreferenceMultiLine(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
    }

    public CheckBoxPreferenceMultiLine(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
    }

    public CheckBoxPreferenceMultiLine(Context ctx) {
        super(ctx);
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);
        TextView textView = (TextView) view.findViewById(android.R.id.title);
        if (textView != null) {
            textView.setSingleLine(false);
        }
    }
}

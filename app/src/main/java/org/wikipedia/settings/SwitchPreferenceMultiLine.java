package org.wikipedia.settings;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.util.AttributeSet;
import android.widget.TextView;

public class SwitchPreferenceMultiLine extends SwitchPreferenceCompat {
    public SwitchPreferenceMultiLine(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
    }

    public SwitchPreferenceMultiLine(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
    }

    public SwitchPreferenceMultiLine(Context ctx) {
        super(ctx);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView textView = holder.itemView.findViewById(android.R.id.title);
        if (textView != null) {
            textView.setSingleLine(false);
        }
    }
}

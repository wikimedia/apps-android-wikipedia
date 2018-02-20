package org.wikipedia.settings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.Toast;

import org.wikipedia.R;

public class PreferenceMultiLine extends Preference {

    public PreferenceMultiLine(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
    }

    public PreferenceMultiLine(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
    }

    public PreferenceMultiLine(Context ctx) {
        super(ctx);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        // Intercept the click listener for this preference, and if the preference has an intent,
        // launch the intent ourselves, so that we can catch the exception if the intent fails.
        // (but only do this if the preference doesn't already have a click listener)
        if (this.getOnPreferenceClickListener() == null) {
            this.setOnPreferenceClickListener((preference) -> {
                if (preference.getIntent() != null) {
                    try {
                        getContext().startActivity(preference.getIntent());
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getContext(), getContext().getString(R.string.error_browser_not_found), Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                return false;
            });
        }
    }
}

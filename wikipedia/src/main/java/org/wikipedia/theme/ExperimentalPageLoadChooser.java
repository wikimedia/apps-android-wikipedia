package org.wikipedia.theme;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.events.ThemeChangeEvent;
import org.wikipedia.settings.Prefs;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

/**
 * Temp class, which enables turning on the page load experiment. Remove once it is completed.
 */
public final class ExperimentalPageLoadChooser {
    public static void initExperimentalPageLoadChooser(final Context context, View root) {
        LinearLayout layout = (LinearLayout) root.findViewById(R.id.experimental_page_load);
        CheckBox expPageLoadCB = (CheckBox) layout.findViewById(R.id.use_exp_page_load_cb);
        expPageLoadCB.setChecked(Prefs.isExperimentalPageLoadEnabled());
        expPageLoadCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.setExperimentalPageLoadEnabled(isChecked);
                WikipediaApp.getInstance().getBus().post(new ThemeChangeEvent());
                // Not ideal since it doesn't automatically reload the page
                // but good enough for experimental switching.
                // (The old strategy has a backstack, the new one doesn't.)
                // A page refresh right after this will crash the app.
                // Better use load a new page instead (Today, Random,  search, ...)
            }
        });
    }

    // do not instantiate
    private ExperimentalPageLoadChooser() {
    }
}

package org.wikipedia;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class ThemedActionBarActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(WikipediaApp.getInstance().getCurrentTheme());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            //for 2.3 it seems to be necessary to set this explicitly:
            getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(Utils.getThemedAttributeId(this, R.attr.actionbar_drawable)));
        }
    }

}

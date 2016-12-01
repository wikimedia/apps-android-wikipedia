package org.wikipedia.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;

public abstract class ThemedActionBarActivity extends ActionBarActivity {
    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
    }

    protected void setTheme() {
        setTheme(WikipediaApp.getInstance().getCurrentTheme().getResourceId());
    }
}

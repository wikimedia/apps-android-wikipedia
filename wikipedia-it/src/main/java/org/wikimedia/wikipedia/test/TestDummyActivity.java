package org.wikimedia.wikipedia.test;

import android.app.Activity;
import android.os.Bundle;

/**
 * Dummy activity that does nothing and is happy about it.
 *
 * Used to do things on the UI thread in unit tests.
 */
public class TestDummyActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
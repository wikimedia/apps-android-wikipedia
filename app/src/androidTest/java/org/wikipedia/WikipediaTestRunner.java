package org.wikipedia;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnitRunner;

import org.wikipedia.dataclient.okhttp.TestStubInterceptor;
import org.wikipedia.espresso.MockInstrumentationInterceptor;

public class WikipediaTestRunner extends AndroidJUnitRunner {
    @Override
    public void onStart() {
        TestStubInterceptor.setCallback(new MockInstrumentationInterceptor(InstrumentationRegistry.getContext()));
        super.onStart();
    }
}

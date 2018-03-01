package org.wikipedia;

import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnitRunner;

import org.wikipedia.dataclient.okhttp.TestStubInterceptor;
import org.wikipedia.espresso.MockInstrumentationInterceptor;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.PrefsIoUtil;

import java.io.File;

import static org.wikipedia.espresso.Constants.TEST_COMPARISON_OUTPUT_FOLDER;

public class WikipediaTestRunner extends AndroidJUnitRunner {
    @Override
    public void onStart() {
        TestStubInterceptor.setCallback(new MockInstrumentationInterceptor(InstrumentationRegistry.getContext()));
        clearAppInfo();
        disableOnboarding();
        cleanUpComparisonResults();

        super.onStart();
    }

    private void disableOnboarding() {
        // main onboarding screen
        Prefs.setInitialOnboardingEnabled(false);

        // onboarding feed cards
        PrefsIoUtil.setBoolean(R.string.preference_key_feed_readinglists_sync_onboarding_card_enabled, false);
        PrefsIoUtil.setBoolean(R.string.preference_key_toc_tutorial_enabled, false);
        PrefsIoUtil.setBoolean(R.string.preference_key_feed_customize_onboarding_card_enabled, false);
        PrefsIoUtil.setBoolean(R.string.preference_key_offline_onboarding_card_enabled, false);
        PrefsIoUtil.setBoolean(R.string.preference_key_select_text_tutorial_enabled, false);
    }

    private void clearAppInfo() {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(WikipediaApp.getInstance());
        prefs.edit().clear().commit();
        WikipediaApp.getInstance().deleteDatabase("wikipedia.db");
    }

    private void cleanUpComparisonResults() {
        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + TEST_COMPARISON_OUTPUT_FOLDER);
        if (folder.exists()) {
            try {
                File[] files = folder.listFiles();
                for (File file : files) {
                    if (file.isFile()) {
                        if (!file.delete()) {
                            throw new RuntimeException("Cannot delete file: " + file.getName() + " while cleaning up");
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to clean up comparison result files: " + e);
            }
        }
    }
}


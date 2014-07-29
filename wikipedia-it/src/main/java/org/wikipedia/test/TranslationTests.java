package org.wikipedia.test;

import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import android.util.DisplayMetrics;
import android.util.Log;
//import org.wikimedia.wikipedia.test.R;
import org.wikipedia.R;
import org.wikipedia.page.PageActivity;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * Tests to make sure that the string resources don't cause any issues. Mainly the goal is to test all tranlsations,
 * but even the default strings are tested.
 *
 * Picked a random Activity but has to be from the app.
 *
 * TODO: check content_license_html is valid HTML
 * TODO: check for missing translations
 */
public class TranslationTests extends ActivityInstrumentationTestCase2<PageActivity> {
    public static final String TAG = "TrTest";
    private PageActivity activity;

    public TranslationTests() {
        super(PageActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();
    }

    public void testPreconditions() {
        assertNotNull(activity);
    }

    public void testAllTranslations() throws Exception {
        AssetManager assetManager = getInstrumentation().getTargetContext().getResources().getAssets();
        for (String lang : assetManager.getLocales()) {
            Log.i(TAG, "----locale=" + (lang.equals("") ? "DEFAULT" : lang));
            setLocale(lang);
//            checkAllStrings(); // might take too long
            if (!lang.startsWith("qq")) {
                checkOneStringWithParameter(R.string.saved_pages_search_empty_message);
                checkOneStringWithParameter(R.string.last_updated_text);
                checkOneStringWithParameter(R.string.editing_error_spamblacklist);
                checkOneStringWithParameter(R.string.history_search_empty_message);
                checkOneStringWithParameter(R.string.page_protected_other);
                // TODO: build a list of all parameterized string resources from default strings dynamically first
            }
        }
    }

    private Locale myLocale;

    public void setLocale(String lang) {
        myLocale = new Locale(lang);
        Resources res = getInstrumentation().getTargetContext().getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
        getInstrumentation().callActivityOnRestart(activity);
    }

    private void checkAllStrings() {
        final R.string stringResources = new R.string();
        final Class<R.string> c = R.string.class;
        final Field[] fields = c.getDeclaredFields();

        for (int i = 0, max = fields.length; i < max; i++) {
            final String name;
            final int resourceId;
            try {
                name = fields[i].getName();
                resourceId = fields[i].getInt(stringResources);
            } catch (Exception e) {
                Log.e(TAG, myLocale + "-" + i + "; failed: " + e.getMessage());
                continue;
            }
            try {
                String value = getInstrumentation().getTargetContext().getResources().getString(resourceId);
//                Log.i(TAG, buildLogString(i, name) + " = " + value);
            } catch (Resources.NotFoundException e) {
                Log.w(TAG, buildLogString(i, name) + "; <not found>");
            } catch (RuntimeException e) {
                Log.e(TAG, buildLogString(i, name) + "; --- " + e.getMessage());
            }
        }
    }

    private String buildLogString(int i, String name) {
        return myLocale + "-" + i + "; name = " + name;
    }

    public void checkOneStringWithParameter(int resourceId) throws Exception {
        final String param1 = "[param1]";
        String translatedString = getInstrumentation().getTargetContext().getString(resourceId, param1);
        Log.i(TAG, myLocale + ":" + translatedString);
        assertTrue(myLocale + ":'" + translatedString + "' doesn't contain " + param1, translatedString.contains(param1));
    }

//    public void testOneStringWithParameter() throws Exception {
//        setLocale("de");
//        assertEquals("Zuletzt aktualisiert foo", getInstrumentation().getTargetContext().getString(R.string.last_updated_text, "foo"));
//        setLocale("fa");
//        assertEquals("", getInstrumentation().getTargetContext().getString(R.string.last_updated_text, "foo"));
//    }
}

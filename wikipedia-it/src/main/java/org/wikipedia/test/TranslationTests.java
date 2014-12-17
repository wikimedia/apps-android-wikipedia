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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private static final String TAG = "TrTest";

    /** Add more if needed, but then also add some tests. */
    private static final String[] POSSIBLE_PARAMS = new String[] {"%s", "%d", "%.2f"};

    private PageActivity activity;
    private StringBuilder mismatches = new StringBuilder();

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
        setLocale(Locale.ROOT.toString());
        List<Res> tagRes = new ResourceCollector("<", "&lt;").collectParameterResources();
        List<Res> noTagRes = new ResourceCollector("<", "&lt;").not().collectParameterResources();
        List<Res> stringParamRes = new ResourceCollector("%s").collectParameterResources();
        List<Res> decimalParamRes = new ResourceCollector("%d").collectParameterResources();
        List<Res> floatParamRes = new ResourceCollector("%.2f").collectParameterResources();

        AssetManager assetManager = getInstrumentation().getTargetContext().getResources().getAssets();
        for (String lang : assetManager.getLocales()) {
            Log.i(TAG, "----locale=" + (lang.equals("") ? "DEFAULT" : lang));
            setLocale(lang);
//            checkAllStrings(); // might take too long

            // commented out during the transition from 1 param to 0
//            checkTranslationHasNoParameter(R.string.saved_pages_search_empty_message);
//            checkTranslationHasNoParameter(R.string.history_search_empty_message);

            if (!lang.startsWith("qq")) {
                // tag (html) parameters
                for (Res res : tagRes) {
                    if (res.id == R.string.wp_stylized
                        && (lang.startsWith("iw") || lang.startsWith("he"))) {
                        // exceptions of the rule
                        continue;
                    }
                    expectContains(res, "<", "&lt;");
                }

                for (Res res : noTagRes) {
                    expectNotContains(res, "<", "&lt;");
                }

                // string parameters
                for (Res res : stringParamRes) {
                    if (res.id == R.string.editing_error_spamblacklist
                        && (lang.startsWith("ak") || lang.startsWith("el"))) {
                        // taking forever to get those fixed :(
                        continue;
                    }
                    checkTranslationHasParameter(res, "%s", "[stringParam]", null);
                }

                // decimal parameters
                for (Res res : decimalParamRes) {
                    final int param1 = 42;
                    checkTranslationHasParameter(res, "%d", param1, null);
                }

                // floating point parameters
                for (Res res : floatParamRes) {
                    final float param1 = .27f;
                    checkTranslationHasParameter(res, "%.2f", param1, "0,27");
                }
            }
        }
        assertTrue("\n" + mismatches.toString(), mismatches.length() == 0);
    }

    private Locale myLocale;

    public void setLocale(String lang) {
        myLocale = new Locale(lang);
        Locale.setDefault(myLocale);
        Resources res = getInstrumentation().getTargetContext().getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
        getInstrumentation().callActivityOnRestart(activity);
    }

    private void checkAllStrings() {
        new ResourceCollector().collectParameterResources();
    }

    private String buildLogString(int i, String name) {
        return myLocale + "-" + i + "; name = " + name;
    }

    private void expectNotContains(Res res, String... examples) {
        String translatedString = getInstrumentation().getTargetContext().getString(res.id);
//        Log.i(TAG, myLocale + ":" + translatedString);
        for (String example : examples) {
            if (translatedString.contains(example)) {
                final String msg = myLocale + ":" + res.name + " = " + translatedString + "' contains " + example;
                Log.e(TAG, msg);
                mismatches.append(msg).append("\n");
                break;
            }
        }
    }

    private void expectContains(Res res, Object... examples) {
        String translatedString = getInstrumentation().getTargetContext().getString(res.id);
//        Log.i(TAG, myLocale + ":" + translatedString);
        boolean found = false;
        for (Object example : examples) {
            if (translatedString.contains(example.toString())) {
                found = true;
                break;
            }
        }
        if (!found) {
            final String msg = myLocale + ":" + res.name + " = " + translatedString + "' does not contain " + Arrays.toString(examples);
            Log.e(TAG, msg);
            mismatches.append(msg).append("\n");
        }
    }

    private void checkTranslationHasNoParameter(Res res) {
        final String val1 = "[val1]";
        String translatedString = getInstrumentation().getTargetContext().getString(res.id, val1);
//        Log.i(TAG, myLocale + ":" + translatedString);
        if (translatedString.contains(val1)) {
            final String msg = myLocale + ":" + res.name + " = " + translatedString + "' contains " + val1;
            Log.e(TAG, msg);
            mismatches.append(msg).append("\n");
        }
    }

    public void checkTranslationHasParameter(Res res, String paramName, Object val1, String alternateFormat) {
//        Log.i(TAG, myLocale + ":" + res.name);
        String translatedString = getInstrumentation().getTargetContext().getString(res.id, val1);
//        Log.d(TAG, translatedString);
        if (!translatedString.contains(String.format(paramName, val1))
            && (alternateFormat == null || !translatedString.contains(alternateFormat))) {
            final String msg = myLocale + ":" + res.name + " = " + translatedString + "' doesn't contain " + val1;
            Log.e(TAG, msg);
            mismatches.append(msg).append("\n");
        }
    }

    private class ResourceCollector {
        private boolean negate;
        private final String[] paramExamples;

        ResourceCollector(String... paramExamples) {
            this.paramExamples = paramExamples;
        }

        private ResourceCollector not() {
            negate = true;
            return this;
        }

        private List<Res> collectParameterResources() {
            final List<Res> resources = new ArrayList<Res>();
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
                    if (name.startsWith("abc_") || name.startsWith("preference_")) {
                        continue; // don't care about appcompat string; and preference string resources don't get translated
                    }

                    assertParameterFormats(name, value);

                    // Find parameter
                    boolean found = findParameter(value);
                    if ((!negate && found) || (negate && !found)) {
                        resources.add(new Res(resourceId, name));
                    }
                } catch (Resources.NotFoundException e) {
                    Log.w(TAG, buildLogString(i, name) + "; <not found>");
                } catch (RuntimeException e) {
                    Log.e(TAG, buildLogString(i, name) + "; --- " + e.getMessage());
                }
            }

            return resources;
        }

        /**
         * If it has a parameter it should be one of POSSIBLE_PARAMS.
         * If not then flag this so we can improve the tests.
         */
        private void assertParameterFormats(String name, String value) {
            if (value.startsWith("Last updated")) {
                System.out.println();
            }
            if (value.contains("%")) {
                boolean ok = false;
                int start = value.indexOf('%');
                for (String possible : POSSIBLE_PARAMS) {
                    int end = value.indexOf(getLastChar(possible), start);
                    if (end != -1 && end < value.length()) {
                        String candidate = value.substring(start, end + 1);
                        System.out.println("candidate = " + candidate);
                        if (possible.equals(candidate)) {
                            ok = true;
                            break;
                        }
                    }
                }
                if (!ok) {
                    fail("Unexpected format in " +  name + ": '" + value + "'. Update tests!");
                }
            }
        }

        private String getLastChar(String str) {
            return str.substring(str.length() - 1);
        }

        private boolean findParameter(String value) {
            boolean found = false;
            for (String paramExample : paramExamples) {
                if (value.contains(paramExample)) {
                    found = true;
                    if (!negate) {
                        break;
                    }
                }
            }
            return found;
        }
    }

    class Res {
        private int id;
        private String name;

        public Res(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}

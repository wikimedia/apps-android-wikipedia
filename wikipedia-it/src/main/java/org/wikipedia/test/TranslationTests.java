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
    public static final String TAG = "TrTest";
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
        List<Res> stringParamRes = new ResourceCollector("%s", "%1$s").collectParameterResources();
        List<Res> decimalParamRes = new ResourceCollector("%d", "%1$d").collectParameterResources();
        List<Res> floatParamRes = new ResourceCollector("%f", "%.2f").collectParameterResources();

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
                    checkTranslationHasParameter(res, "[stringParam]", null);
                }

                if (lang.startsWith("fa") || lang.startsWith("ar") || lang.equals("bn")
                    || lang.equals("mr") || lang.equals("my") || lang.startsWith("ne") || lang.equals("ps")) {
                    // don't check the number params since those languages have different string representations for numbers
                    continue;
                }

                // decimal parameters
                for (Res res : decimalParamRes) {
                    final int param1 = 42;
                    checkTranslationHasParameter(res, param1, null);
                }

                // float parameters
                for (Res res : floatParamRes) {
                    final float param1 = .27f;
                    checkTranslationHasParameter(res, param1, "0,27");
//                // TODO: build a list of all parameterized string resources from default strings dynamically first
                }
            }
        }
        assertTrue("\n" + mismatches.toString(), mismatches.length() == 0);
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
        final String param1 = "[param1]";
        String translatedString = getInstrumentation().getTargetContext().getString(res.id, param1);
//        Log.i(TAG, myLocale + ":" + translatedString);
        if (translatedString.contains(param1)) {
            final String msg = myLocale + ":" + res.name + " = " + translatedString + "' contains " + param1;
            Log.e(TAG, msg);
            mismatches.append(msg).append("\n");
        }
    }

    public void checkTranslationHasParameter(Res res, Object param1, String alternate) throws Exception {
        String translatedString = getInstrumentation().getTargetContext().getString(res.id, param1);
//        Log.i(TAG, myLocale + ":" + translatedString);
        if (!translatedString.contains(param1.toString())
            && (alternate == null || !translatedString.contains(alternate))) {
            final String msg = myLocale + ":" + res.name + " = " + translatedString + "' doesn't contain " + param1;
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
//                Log.i(TAG, buildLogString(i, name) + " = " + value);
                    if (!name.startsWith("abc_") && !name.startsWith("preference_")) {
                        boolean found = false;
                        if (value.contains("wikimediafoundation.org") && negate) {
                            System.out.println("found = " + found);
                        }
                        for (String paramExample : paramExamples) {
                            if (value.contains(paramExample)) {
                                found = true;
                                if (!negate) {
                                    break;
                                }
                            }
                        }
                        if ((!negate && found) || (negate && !found)) {
                            resources.add(new Res(resourceId, name));
                        }
                    }
                } catch (Resources.NotFoundException e) {
                    Log.w(TAG, buildLogString(i, name) + "; <not found>");
                } catch (RuntimeException e) {
                    Log.e(TAG, buildLogString(i, name) + "; --- " + e.getMessage());
                }
            }

            return resources;
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

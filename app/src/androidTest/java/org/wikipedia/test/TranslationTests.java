package org.wikipedia.test;

import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.test.filters.SmallTest;
import android.util.DisplayMetrics;

import org.junit.Test;
import org.wikipedia.R;
import org.wikipedia.model.BaseModel;
import org.wikipedia.util.ConfigurationCompat;
import org.wikipedia.util.log.L;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

/**
 * Tests to make sure that the string resources don't cause any issues. Mainly the goal is to test
 * all translations, but even the default strings are tested.
 *
 * TODO: check content_license_html is valid HTML
 */
@SmallTest
public class TranslationTests {
    /** Add more if needed, but then also add some tests. */
    private static final String[] POSSIBLE_PARAMS = new String[] {"%s", "%1$s", "%2$s", "%1$d", "%2$d", "%d", "%.2f", "^1"};

    private final StringBuilder mismatches = new StringBuilder();

    @Test public void testAllTranslations() {
        setLocale(Locale.ROOT.toString());
        String defaultLang = Locale.getDefault().getLanguage();
        List<Res> tagRes = new ResourceCollector("<", "&lt;").collectParameterResources(defaultLang);
        List<Res> noTagRes = new ResourceCollector("<", "&lt;").not().collectParameterResources(defaultLang);
        List<Res> stringParamRes = new ResourceCollector("%s").collectParameterResources(defaultLang);
        List<Res> twoStringParamRes = new ResourceCollector("%2$s").collectParameterResources(defaultLang);
        List<Res> twoDecimalParamRes = new ResourceCollector("%2$d").collectParameterResources(defaultLang);
        List<Res> decimalParamRes = new ResourceCollector("%d").collectParameterResources(defaultLang);
        List<Res> floatParamRes = new ResourceCollector("%.2f").collectParameterResources(defaultLang);
        List<Res> textUtilTemplateParamRes = new ResourceCollector("^1").collectParameterResources(defaultLang);
        // todo: flag usage of templates {{}}.

        AssetManager assetManager = getResources().getAssets();
        for (String lang : assetManager.getLocales()) {
            L.i("----locale=" + (lang.equals("") ? "DEFAULT" : lang));
            setLocale(lang);
            checkAllStrings(lang);

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
                    checkTranslationHasParameter(res, "%s", "[stringParam]", null);
                }

                // 2 string parameters
                for (Res res : twoStringParamRes) {
                    checkTranslationHasTwoParameters(res, "%s", "[stringParam1]", "[stringParam2]");
                }

                // decimal parameters
                for (Res res : decimalParamRes) {
                    final int param1 = 42;
                    checkTranslationHasParameter(res, "%d", param1, null);
                }

                // 2 decimal parameters
                for (Res res : twoDecimalParamRes) {
                    final int param1 = 42;
                    final int param2 = 8675309;
                    checkTranslationHasTwoParameters(res, "%d", param1, param2);
                }

                // floating point parameters
                for (Res res : floatParamRes) {
                    final float param1 = .27f;
                    checkTranslationHasParameter(res, "%.2f", param1, "0,27");
                }

                // template format for com.android.TextUtils.expandTemplate
                for (Res res : textUtilTemplateParamRes) {
                    checkTranslationHasParameter(res, "^1", "[templateParam]", null);
                }
            }
        }
        assertThat("\n" + mismatches.toString(), mismatches.length(), is(0));
    }

    private Locale myLocale;

    private void setLocale(String lang) {
        myLocale = new Locale(lang);
        Locale.setDefault(myLocale);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        Configuration conf = getResources().getConfiguration();
        ConfigurationCompat.setLocale(conf, myLocale);
        getResources().updateConfiguration(conf, dm);
    }

    private void checkAllStrings(String lang) {
        new ResourceCollector().collectParameterResources(lang);
    }

    private String buildLogString(int i, String name) {
        return myLocale + "-" + i + "; name = " + name;
    }

    private void expectNotContains(Res res, String... examples) {
        String translatedString = getString(res.id);
//        L.i(myLocale + ":" + translatedString);
        for (String example : examples) {
            if (translatedString.contains(example)) {
                final String msg = myLocale + ":" + res.name + " = " + translatedString + "' contains " + example;
                L.e(msg);
                mismatches.append(msg).append("\n");
                break;
            }
        }
    }

    private void expectContains(Res res, Object... examples) {
        String translatedString = getString(res.id);
//        L.i(myLocale + ":" + translatedString);
        boolean found = false;
        for (Object example : examples) {
            if (translatedString.contains(example.toString())) {
                found = true;
                break;
            }
        }
        if (!found) {
            final String msg = myLocale + ":" + res.name + " = " + translatedString + "' does not contain " + Arrays.toString(examples);
            L.e(msg);
            mismatches.append(msg).append("\n");
        }
    }

    private void checkTranslationHasNoParameter(Res res) {
        final String val1 = "[val1]";
        String translatedString = getString(res.id, val1);
//        L.i(myLocale + ":" + translatedString);
        if (translatedString.contains(val1)) {
            final String msg = myLocale + ":" + res.name + " = " + translatedString + "' contains " + val1;
            L.e(msg);
            mismatches.append(msg).append("\n");
        }
    }

    private void checkTranslationHasParameter(Res res, String paramName, Object val1, String alternateFormat) {
//        L.i(myLocale + ":" + res.name + ":" + paramName);
        String translatedString = getString(res.id, val1);
//        L.d(translatedString);
        if (!translatedString.contains(String.format(paramName, val1))
            && (alternateFormat == null || !translatedString.contains(alternateFormat))) {
            final String msg = myLocale + ":" + res.name + " = " + translatedString + "' is missing " + val1;
            L.e(msg);
            mismatches.append(msg).append("\n");
        }
    }

    private void checkTranslationHasTwoParameters(Res res, String paramName, Object val1, Object val2) {
        L.i(myLocale + ":" + res.name + ":" + paramName);
        String translatedString = getString(res.id, val1, val2);
        L.d(translatedString);
        if (!translatedString.contains(String.format(paramName, val1))
                || !translatedString.contains(String.format(paramName, val2))) {
            final String msg = myLocale + ":" + res.name + " = " + translatedString
                    + "' is missing " + val1
                    + "' or " + val2;
            L.e(msg);
            mismatches.append(msg).append("\n");
        }
    }

    @NonNull private String getString(@StringRes int id, Object... args) {
        return getTargetContext().getString(id, args);
    }

    @NonNull private String getString(@StringRes int id) {
        return getTargetContext().getString(id);
    }

    @NonNull private Resources getResources() {
        return getTargetContext().getResources();
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

        private List<Res> collectParameterResources(String lang) {
            final List<Res> resources = new ArrayList<>();
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
                    L.e(myLocale + "-" + i + "; failed: " + e.getMessage());
                    continue;
                }
                // todo: don't try. Die.
                try {
                    String value = getString(resourceId);
                    // don't care about appcompat string; and preference string resources don't get translated
                    if (name.startsWith("abc_")
                    ||  name.startsWith("preference_")
                    // Required after upgrading Support Libraries from v23.0.1 to v23.1.0.
                    || name.equals("character_counter_pattern")
                    || name.startsWith("hockeyapp_")
                    || name.equals("find_in_page_result")) {
                        continue;
                    }

                    assertParameterFormats(lang, name, value);

                    // Find parameter
                    boolean found = findParameter(value);
                    if ((!negate && found) || (negate && !found)) {
                        resources.add(new Res(resourceId, name));
                    }
                } catch (Resources.NotFoundException e) {
                    L.w(buildLogString(i, name) + "; <not found>");
                } catch (RuntimeException e) {
                    L.e(buildLogString(i, name) + "; --- " + e.getMessage());
                }
            }

            return resources;
        }

        /**
         * If it has a parameter it should be one of POSSIBLE_PARAMS.
         * If not then flag this so we can improve the tests.
         */
        private void assertParameterFormats(String lang, String name, String value) {
            if (value.startsWith("Last updated")) {
                L.d("");
            }
            if (value.contains("%")) {
                boolean ok = false;
                int start = value.indexOf('%');
                for (String possible : POSSIBLE_PARAMS) {
                    int end = value.indexOf(getLastChar(possible), start);
                    if (end != -1 && end < value.length()) {
                        String candidate = value.substring(start, end + 1);
                        L.d("candidate = " + candidate);
                        if (possible.equals(candidate)) {
                            ok = true;
                            break;
                        }
                    }
                }
                if (!ok) {
                    fail("Unexpected format in " +  name + " (" + lang + "): '" + value + "'. Update tests!");
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

    private static class Res extends BaseModel {
        private final int id;
        private final String name;

        Res(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}

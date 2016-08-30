package org.wikipedia.test;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;

import com.facebook.testing.screenshot.Screenshot;
import com.facebook.testing.screenshot.ViewHelpers;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.runner.RunWith;
import org.wikipedia.theme.Theme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@RunWith(Theories.class) public abstract class ViewTest {
    protected static final int WIDTH_DP_XL = 720;
    protected static final int WIDTH_DP_L = 480;
    protected static final int WIDTH_DP_M = 320;
    protected static final int WIDTH_DP_S = 240;
    protected static final int WIDTH_DP_XS = 120;

    protected enum LayoutDirection { LOCALE, RTL }
    protected enum Select { SELECTED, DESELECTED }

    private int widthDp;
    private Locale locale;
    private LayoutDirection layoutDirection;
    private Theme theme;
    private Context ctx;

    @DataPoints public static final Locale[] LOCALES = {Locale.ENGLISH};
    @DataPoints public static final LayoutDirection[] LAYOUT_DIRECTIONS = LayoutDirection.values();
    @DataPoints public static final Theme[] THEMES = Theme.values();
    @DataPoints public static final Select[] SELECTS = Select.values();

    protected void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                         @NonNull Theme theme) {
        setUp(widthDp, LOCALES[0], layoutDirection, theme);
    }

    protected void setUp(int widthDp, @NonNull Locale locale,
                         @NonNull LayoutDirection layoutDirection, @NonNull Theme theme) {
        this.widthDp = widthDp;
        this.locale = locale;
        this.layoutDirection = layoutDirection;
        this.theme = theme;
        ctx = new ContextThemeWrapper(InstrumentationRegistry.getTargetContext(),
                theme.getResourceId());
        Locale.setDefault(locale);
        config(widthDp, ctx, locale);
    }

    protected void snap(@NonNull View subject, @Nullable String... dataPoints) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            int rtl = layoutDirection == LayoutDirection.RTL
                    ? ViewCompat.LAYOUT_DIRECTION_RTL
                    : TextUtilsCompat.getLayoutDirectionFromLocale(locale);
            //noinspection WrongConstant
            subject.setLayoutDirection(rtl);
        }

        ViewHelpers.setupView(subject).setExactWidthDp(widthDp).layout();

        List<String> list = new ArrayList<>();
        list.add(widthDp + "dp");
        list.add(locale.toString().toLowerCase());
        list.add(layoutDirection == LayoutDirection.RTL ? "rtl" : "ltr");
        list.add(theme.toString().toLowerCase());
        list.addAll(Arrays.asList(ArrayUtils.nullToEmpty(dataPoints)));
        Screenshot.snap(subject).setName(testName(list)).record();
    }

    protected Context ctx() {
        return ctx;
    }

    private void config(int widthDp, @NonNull Context ctx, @NonNull Locale locale) {
        Configuration cfg = new Configuration(ctx.getResources().getConfiguration());
        cfg.screenWidthDp = widthDp;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            cfg.setLocales(new LocaleList(locale));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            cfg.setLocale(locale);
            cfg.setLayoutDirection(locale);
        } else {
            //noinspection deprecation
            cfg.locale = locale;
        }

        ctx.getResources().updateConfiguration(cfg, null);
    }

    // todo: identify method name by @Theory / @Test name instead of depth
    private String testName(@NonNull Iterable<String> dataPoints) {
        final int depth = 4;
        StackTraceElement element = Thread.currentThread().getStackTrace()[depth];

        String name = element.getClassName() + '.' + element.getMethodName();
        for (String dataPoint : dataPoints) {
            name += '-' + dataPoint;
        }

        return name;
    }
}
package org.wikipedia.test.view;

import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.LocaleList;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.text.TextUtilsCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;

import com.facebook.common.util.UriUtil;
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

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static butterknife.ButterKnife.findById;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.wikipedia.test.TestUtil.runOnMainSync;
import static org.wikipedia.util.StringUtil.emptyIfNull;

@RunWith(Theories.class) public abstract class ViewTest {
    @DataPoints public static final Locale[] LOCALES = {Locale.ENGLISH};
    @DataPoints public static final LayoutDirection[] LAYOUT_DIRECTIONS = LayoutDirection.values();
    @DataPoints public static final FontScale[] FONT_SCALES = FontScale.values();
    @DataPoints public static final Theme[] THEMES = Theme.values();
    @DataPoints public static final PrimaryTestStr[] PRIMARY_STRS = PrimaryTestStr.values();
    @DataPoints public static final SecondaryTestStr[] SECONDARY_STRS = SecondaryTestStr.values();
    @DataPoints public static final PrimaryTestImg[] PRIMARY_IMGS = PrimaryTestImg.values();
    @DataPoints public static final SecondaryTestImg[] SECONDARY_IMGS = SecondaryTestImg.values();

    protected static final int WIDTH_DP_XL = 720;
    protected static final int WIDTH_DP_L = 480;
    protected static final int WIDTH_DP_M = 320;
    protected static final int WIDTH_DP_S = 240;
    protected static final int WIDTH_DP_XS = 120;

    private int widthDp;
    private Locale locale;
    private LayoutDirection layoutDirection;
    private FontScale fontScale;
    private Theme theme;
    private Context ctx;

    protected void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                         @NonNull FontScale fontScale, @NonNull Theme theme) {
        setUp(widthDp, LOCALES[0], layoutDirection, fontScale, theme);
    }

    protected void setUp(int widthDp, @NonNull Locale locale,
                         @NonNull LayoutDirection layoutDirection, @NonNull FontScale fontScale,
                         @NonNull Theme theme) {
        this.widthDp = widthDp;
        this.locale = locale;
        this.layoutDirection = layoutDirection;
        this.fontScale = fontScale;
        this.theme = theme;
        ctx = new ContextThemeWrapper(getTargetContext(), theme.getResourceId());
        Locale.setDefault(locale);
        config();
    }

    protected void snap(@NonNull View subject, @Nullable String... dataPoints) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            int rtl = layoutDirection == LayoutDirection.RTL
                    ? View.LAYOUT_DIRECTION_RTL
                    : TextUtilsCompat.getLayoutDirectionFromLocale(locale);
            //noinspection WrongConstant
            subject.setLayoutDirection(rtl);
        }

        ViewHelpers.setupView(subject).setExactWidthDp(widthDp).layout();

        List<String> list = new ArrayList<>();
        list.add(widthDp + "dp");
        list.add(locale.toString());
        list.add(layoutDirection == LayoutDirection.RTL ? "rtl" : "ltr");
        list.add("font" + fontScale.multiplier() + "x");
        list.add(theme.toString().toLowerCase());
        list.addAll(Arrays.asList(ArrayUtils.nullToEmpty(dataPoints)));
        Screenshot.snap(subject).setName(testName(list)).record();
    }

    protected void requestFocus(@NonNull final View view) {
        runOnMainSync(new Runnable() {
            @Override public void run() {
                view.requestFocus();
            }
        });
    }

    protected void assertText(@NonNull View subject, @IdRes int id, @NonNull TestStr text) {
        assertText(subject, id, text.id());
    }

    protected void assertText(@NonNull View subject, @IdRes int id, @StringRes int text) {
        TextView textView = findById(subject, id);
        assertThat(textView.getText().toString(), is(emptyIfNull(str(text))));
    }

    @Nullable protected Uri frescoUri(@DrawableRes int id) {
        return id == 0 ? null : new Uri.Builder()
                .scheme(UriUtil.LOCAL_RESOURCE_SCHEME)
                .path(String.valueOf(id))
                .build();
    }

    protected String str(@NonNull TestStr str, @Nullable Object... formatArgs) {
        return str(str.id(), formatArgs);
    }

    protected String str(@StringRes int id, @Nullable Object... formatArgs) {
        return id == 0 ? null : ctx().getString(id, formatArgs);
    }

    protected Context ctx() {
        return ctx;
    }

    private void config() {
        Configuration cfg = new Configuration(ctx.getResources().getConfiguration());
        cfg.screenWidthDp = widthDp;
        cfg.fontScale = fontScale.multiplier();
        setConfigLocale(cfg, locale);

        ctx.getResources().updateConfiguration(cfg, null);
    }

    private void setConfigLocale(@NonNull Configuration config, @NonNull Locale locale) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(locale));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
            config.setLayoutDirection(locale);
        } else {
            //noinspection deprecation
            config.locale = locale;
        }
    }

    // todo: identify method name by @Theory / @Test annotation instead of depth and remove repeated
    //       calls to snap()
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

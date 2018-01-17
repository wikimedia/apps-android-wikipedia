package org.wikipedia.zero;

import android.net.Uri;
import android.support.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.TestFileUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@SuppressWarnings("checkstyle:magicnumber")
@RunWith(RobolectricTestRunner.class)
public class ZeroConfigTypeAdapterTest {
    private static final int CYAN = -16711681;
    private static final int WHITE = -1;

    @Test
    public void testEligible() throws Throwable {
        ZeroConfig config = unmarshal("wikipedia_zero_test_eligible.json");

        assertThat(config, notNullValue());
        assertThat(config.toString().length(), greaterThan(0));
        assertThat(config.getMessage(), is("Overstay your stay!"));
        assertThat(config.getBackground(), is(CYAN));
        assertThat(config.getForeground(), is(WHITE));
        assertThat(config.getExitTitle(), is("You are leaving free Wikipedia service"));
        assertThat(config.getExitWarning(), is("Data charges will be applied to your account"));
        assertThat(config.getPartnerInfoText(), is("Learn more at zero.wikimedia.org"));
        assertThat(config.getPartnerInfoUrl(), is(Uri.parse("https://zero.wikimedia.org")));
        assertThat(config.getBannerUrl(), is(Uri.parse("https://zero.wikimedia.org")));
    }

    // TODO: Write a generic unmarshalFromFile method that can be reused to return any class (see
    // similar unmarshal() method in MostReadArticlesTest)
    @NonNull private ZeroConfig unmarshal(@NonNull String filename) throws Throwable {
        String json = TestFileUtil.readRawFile(filename);
        return GsonUnmarshaller.unmarshal(ZeroConfig.class, json);
    }
}

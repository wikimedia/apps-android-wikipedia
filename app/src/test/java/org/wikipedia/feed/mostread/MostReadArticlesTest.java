package org.wikipedia.feed.mostread;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.TestFileUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("checkstyle:magicnumber")
public class MostReadArticlesTest {
    @NonNull public static MostReadArticles unmarshal(@NonNull String filename) throws Throwable {
        String json = TestFileUtil.readRawFile(filename);
        return GsonUnmarshaller.unmarshal(MostReadArticles.class, json);
    }

    @Test public void testUnmarshalManyArticles() throws Throwable {
        MostReadArticles subject = unmarshal("most_read.json");

        assertThat(subject.date(), is(date("2016-06-01Z")));

        assertThat(subject.articles(), notNullValue());
        assertThat(subject.articles().size(), is(40));
    }

    @NonNull private Date date(@NonNull String str) throws Throwable {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'Z'", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.parse(str);
    }
}

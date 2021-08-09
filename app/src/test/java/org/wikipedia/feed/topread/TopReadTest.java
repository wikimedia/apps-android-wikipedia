package org.wikipedia.feed.topread;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.json.MoshiUtil;
import org.wikipedia.test.TestFileUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("checkstyle:magicnumber")
public class TopReadTest {
    @NonNull public static TopRead unmarshal(@NonNull String filename) throws Throwable {
        final String json = TestFileUtil.readRawFile(filename);
        return Objects.requireNonNull(MoshiUtil.getDefaultMoshi().adapter(TopRead.class)
                .fromJson(json));
    }

    @Test public void testUnmarshalManyArticles() throws Throwable {
        TopRead subject = unmarshal("most_read.json");

        assertThat(subject.getDate(), is(date("2016-06-01Z")));

        assertThat(subject.getArticles(), notNullValue());
        assertThat(subject.getArticles().size(), is(40));
    }

    @NonNull private Date date(@NonNull String str) throws Throwable {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'Z'", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.parse(str);
    }
}

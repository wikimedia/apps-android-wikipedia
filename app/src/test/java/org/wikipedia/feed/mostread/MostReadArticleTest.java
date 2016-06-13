package org.wikipedia.feed.mostread;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.test.TestRunner;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@RunWith(TestRunner.class)
@SuppressWarnings("checkstyle:magicnumber")
public class MostReadArticleTest {
    private List<MostReadArticle> subjects;

    @Before public void setUp() throws Throwable {
        subjects = MostReadArticlesTest.unmarshal("most_read.json").articles();
    }

    @Test public void testUnmarshalThumbnails() throws Throwable {
        MostReadArticle subject = subjects.get(3);

        assertThat(subject.normalizedTitle(), is("Marilyn Monroe"));
        assertThat(subject.title(), is("Marilyn_Monroe"));
        assertThat(subject.description(), is("American actress, model, and singer"));
        assertThat(subject.pageId(), is(19318));

        Uri thumbnail = Uri.parse("https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/Marilyn_Monroe_in_1952.jpg/229px-Marilyn_Monroe_in_1952.jpg");
        assertThat(subject.thumbnail(), is(thumbnail));

        assertThat(subject.rank(), is(8));
        assertThat(subject.views(), is(201439));
    }

    @Test public void testUnmarshalNoThumbnails() {
        MostReadArticle subject = subjects.get(0);

        assertThat(subject.normalizedTitle(), is("Bicycle Race"));
        assertThat(subject.title(), is("Bicycle_Race"));
        assertThat(subject.description(), is("rock song by Queen"));
        assertThat(subject.pageId(), is(3957496));
        assertThat(subject.thumbnail(), nullValue());
        assertThat(subject.rank(), is(3));
        assertThat(subject.views(), is(330200));
    }
}
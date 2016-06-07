package org.wikipedia.feed.mostread;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.test.TestRunner;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

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

        Map<Integer, URL> thumbnails = new HashMap<>();
        thumbnails.put(60, new URL("http://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/Marilyn_Monroe_in_1952.jpg/60px-Marilyn_Monroe_in_1952.jpg"));
        thumbnails.put(120, new URL("http://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/Marilyn_Monroe_in_1952.jpg/120px-Marilyn_Monroe_in_1952.jpg"));
        thumbnails.put(320, new URL("http://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/Marilyn_Monroe_in_1952.jpg/229px-Marilyn_Monroe_in_1952.jpg"));

        assertThat(subject.thumbnails(), is(thumbnails));

        assertThat(subject.rank(), is(8));
        assertThat(subject.views(), is(201439));
    }

    @Test public void testUnmarshalNoThumbnails() {
        MostReadArticle subject = subjects.get(0);

        assertThat(subject.normalizedTitle(), is("Bicycle Race"));
        assertThat(subject.title(), is("Bicycle_Race"));
        assertThat(subject.description(), is("rock song by Queen"));
        assertThat(subject.pageId(), is(3957496));
        assertThat(subject.thumbnails(), notNullValue());
        assertThat(subject.thumbnails().size(), is(0));
        assertThat(subject.rank(), is(3));
        assertThat(subject.views(), is(330200));
    }
}
package org.wikipedia.page;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(RobolectricTestRunner.class) public class SectionTest {
    @Test public void testSectionLead() {
        // Section 0 is the lead
        Section section = new Section(0, 0, "Heading", "Heading", "Content");
        assertThat(section.isLead(), is(true));

        // Section 1 is not
        section = new Section(1, 1, "Heading", "Heading", "Content");
        assertThat(section.isLead(), is(false));

        // Section 1 is not, even if it's somehow at level 0
        section = new Section(1, 0, "Heading", "Heading", "Content");
        assertThat(section.isLead(), is(false));
    }

    @Test public void testJSONSerialization() {
        Section parentSection = new Section(1, 1, null, null, "Hi there!");

        assertThat(parentSection, is(Section.fromJson(parentSection.toJSON())));
    }
}

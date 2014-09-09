package org.wikipedia.beta.test;

import android.test.AndroidTestCase;
import org.junit.Test;
import org.wikipedia.beta.page.Section;

public class SectionTests extends AndroidTestCase {
    @Test
    public void testSectionLead() {
        // Section 0 is the lead
        Section section = new Section(0, 0, "Heading", "Heading", "Content");
        assertTrue(section.isLead());

        // Section 1 is not
        section = new Section(1, 1, "Heading", "Heading", "Content");
        assertFalse(section.isLead());

        // Section 1 is not, even if it's somehow at level 0
        section = new Section(1, 0, "Heading", "Heading", "Content");
        assertFalse(section.isLead());
    }

    @Test
    public void testJSONSerialization() throws Exception {
        Section parentSection = new Section(1, 1, null, null, "Hi there!");

        assertEquals(parentSection, new Section(parentSection.toJSON()));
    }

}
package org.wikipedia.test;

import android.test.AndroidTestCase;
import org.junit.Test;
import org.wikipedia.page.Section;

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
        for (int i = 2; i <= 10; i++) {
            parentSection.insertSection(new Section(i, 1, "Something " + i, "Something_" + i, "Content Something" + i));
        }

        assertEquals(parentSection, new Section(parentSection.toJSON()));
    }

}
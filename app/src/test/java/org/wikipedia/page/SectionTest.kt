package org.wikipedia.page

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SectionTest {
    @Test
    fun testSectionLead() {
        // Section 0 is the lead
        var section = Section(0, 0, "Heading", "Heading", "Content")
        assertTrue(section.isLead)

        // Section 1 is not
        section = Section(1, 1, "Heading", "Heading", "Content")
        assertFalse(section.isLead)

        // Section 1 is not, even if it's somehow at level 0
        section = Section(1, 0, "Heading", "Heading", "Content")
        assertFalse(section.isLead)
    }
}

package org.wikipedia.page

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SectionTest {
    @Test
    fun testSectionLead() {
        // Section 0 is the lead
        var section = Section(0, 0, "Heading", "Heading", "Content")
        MatcherAssert.assertThat(section.isLead, Matchers.`is`(true))

        // Section 1 is not
        section = Section(1, 1, "Heading", "Heading", "Content")
        MatcherAssert.assertThat(section.isLead, Matchers.`is`(false))

        // Section 1 is not, even if it's somehow at level 0
        section = Section(1, 0, "Heading", "Heading", "Content")
        MatcherAssert.assertThat(section.isLead, Matchers.`is`(false))
    }
}

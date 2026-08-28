package org.wikimedia.testkitchen.config

import org.wikimedia.testkitchen.config.curation.CollectionCurationRules
import org.wikimedia.testkitchen.config.curation.CurationRules

object CurationFilterFixtures {
    val curationFilter: CurationFilter
        get() = CurationFilter().also {
            it.pageTitleRules = CurationRules<String>().also { rules ->
                rules.isEquals = "Test"
            }
            it.performerGroupsRules = CollectionCurationRules<String>().also { rules ->
                rules.doesNotContain = "sysop"
                rules.containsAny = listOf("steward", "bureaucrat")
            }
        }
}

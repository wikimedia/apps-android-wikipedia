package org.wikimedia.metrics_platform.config;

import java.util.Arrays;

import org.wikimedia.metrics_platform.config.curation.CollectionCurationRules;
import org.wikimedia.metrics_platform.config.curation.CurationRules;

public final class CurationFilterFixtures {

    private CurationFilterFixtures() {
        // Utility class - should never be instantiated
    }

    public static CurationFilter getCurationFilter() {
        return CurationFilter.builder()
                .pageTitleRules(CurationRules.<String>builder().isEquals("Test").build())
                .performerGroupsRules(
                        CollectionCurationRules.<String>builder()
                                .doesNotContain("sysop")
                                .containsAny(Arrays.asList("steward", "bureaucrat"))
                                .build()
                )
                .build();
    }
}

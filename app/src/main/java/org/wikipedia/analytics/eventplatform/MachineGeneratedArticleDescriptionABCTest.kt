package org.wikipedia.analytics.eventplatform

import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil

class MachineGeneratedArticleDescriptionABCTest : ABTest(MACHINE_GEN_DESC, GROUP_SIZE_2) {

    override fun assignGroup() {
        super.assignGroup()
        if (testGroup == GROUP_2 && AccountUtil.isLoggedIn && MachineGeneratedArticleDescriptionsAnalyticsHelper.isUserExperienced) {
            testGroup = GROUP_3
        }
        MachineGeneratedArticleDescriptionsAnalyticsHelper.logUserGroupAssigned(WikipediaApp.instance, testGroup)
    }

    companion object {
        const val MACHINE_GEN_DESC = "mBART25"
    }
}

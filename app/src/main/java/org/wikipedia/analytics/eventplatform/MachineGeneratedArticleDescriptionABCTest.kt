package org.wikipedia.analytics.eventplatform

import kotlinx.coroutines.runBlocking
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.util.log.L

class MachineGeneratedArticleDescriptionABCTest : ABTest(MACHINE_GEN_DESC, GROUP_SIZE_2) {

    override fun assignGroup() {
        super.assignGroup()
        if (testGroup == GROUP_2 && AccountUtil.isLoggedIn) {
            runBlocking {
                try {
                    if (MachineGeneratedArticleDescriptionsAnalyticsHelper.isUserExperienced()) {
                        testGroup = GROUP_3
                    }
                    MachineGeneratedArticleDescriptionsAnalyticsHelper.logUserGroupAssigned(WikipediaApp.instance, testGroup)
                } catch (e: Exception) {
                    L.e(e)
                }
            }
        }
    }

    companion object {
        const val MACHINE_GEN_DESC = "mBART25"
    }
}

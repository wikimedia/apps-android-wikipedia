package org.wikipedia.activitytab

import org.wikipedia.analytics.ABTest
import org.wikipedia.util.ReleaseUtil

class ActivityTabABTest : ABTest("activityTab", GROUP_SIZE_2) {
    fun isInTestGroup(): Boolean {
        return group == GROUP_2 &&
                ReleaseUtil.isPreBetaRelease // TODO: remove before releasing
    }

    fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "activity_tab_b"
            else -> "activity_tab_a"
        }
    }
}

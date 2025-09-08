package org.wikipedia.activitytab

import org.wikipedia.analytics.ABTest

class ActivityTabABTest : ABTest("activityTab", GROUP_SIZE_2) {
    fun isInTestGroup(): Boolean {
        return group == GROUP_2
    }
}

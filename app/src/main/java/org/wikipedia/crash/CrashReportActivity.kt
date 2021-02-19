package org.wikipedia.crash

import org.wikipedia.activity.SingleFragmentActivity

class CrashReportActivity : SingleFragmentActivity<CrashReportFragment>() {
    override fun createFragment(): CrashReportFragment {
        return CrashReportFragment.newInstance()
    }
}

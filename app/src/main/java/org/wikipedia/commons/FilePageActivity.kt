package org.wikipedia.commons

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ResourceUtil

class FilePageActivity : SingleFragmentActivity<FilePageFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setImageZoomHelper()
        setStatusBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        ImageRecommendationsEvent.logImpression("imagedetails_dialog")
    }

    override fun createFragment(): FilePageFragment {
        return FilePageFragment.newInstance(intent.parcelableExtra(Constants.ARG_TITLE)!!,
                intent.getBooleanExtra(INTENT_EXTRA_ALLOW_EDIT, true))
    }

    companion object {
        const val INTENT_EXTRA_ALLOW_EDIT = "allowEdit"

        fun newIntent(context: Context, pageTitle: PageTitle, allowEdit: Boolean = true): Intent {
            return Intent(context, FilePageActivity::class.java)
                    .putExtra(Constants.ARG_TITLE, pageTitle)
                    .putExtra(INTENT_EXTRA_ALLOW_EDIT, allowEdit)
        }
    }
}

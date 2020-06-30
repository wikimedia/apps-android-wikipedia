package org.wikipedia.commons

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ImageZoomHelper

class FilePageActivity : SingleFragmentActivity<FilePageFragment>() {

    private lateinit var imageZoomHelper: ImageZoomHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageZoomHelper = ImageZoomHelper(this)
        setStatusBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
    }

    override fun createFragment(): FilePageFragment {
        return FilePageFragment.newInstance(intent.getParcelableExtra(INTENT_EXTRA_PAGE_TITLE)!!)
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        try {
            return imageZoomHelper.onDispatchTouchEvent(event) || super.dispatchTouchEvent(event)
        } catch (e: Exception) { }
        return false
    }

    companion object {
        const val INTENT_EXTRA_PAGE_TITLE = "pageTitle"
        @JvmStatic
        fun newIntent(context: Context, pageTitle: PageTitle): Intent {
            return Intent(context, FilePageActivity::class.java).putExtra(INTENT_EXTRA_PAGE_TITLE, pageTitle)
        }
    }
}

package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ImageZoomHelper


class SuggestedEditsFeedCardImageTagActivity : BaseActivity() {

    private lateinit var imageZoomHelper: ImageZoomHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = getString(R.string.suggested_edits_tag_images)
        imageZoomHelper = ImageZoomHelper(this)
        setStatusBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        setContentView(R.layout.activity_suggested_edits_feed_card_image_tags)

    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        try {
            return imageZoomHelper.onDispatchTouchEvent(event) || super.dispatchTouchEvent(event)
        } catch (e: Exception) {
        }
        return false
    }

    companion object {

        fun newIntent(context: Context, action: DescriptionEditActivity.Action): Intent {
            return Intent(context, SuggestedEditsFeedCardImageTagActivity::class.java).putExtra(Constants.INTENT_EXTRA_ACTION, action)
        }
    }
}

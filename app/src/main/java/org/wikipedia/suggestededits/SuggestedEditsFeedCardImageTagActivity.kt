package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View.GONE
import android.view.View.VISIBLE
import kotlinx.android.synthetic.main.activity_suggested_edits_feed_card_image_tags.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ImageZoomHelper


class SuggestedEditsFeedCardImageTagActivity : BaseActivity() {

    private lateinit var imageZoomHelper: ImageZoomHelper
    private var suggestedEditsImageTagsFragment: SuggestedEditsImageTagsFragment? = null
    var langFromCode: String = WikipediaApp.getInstance().language().appLanguageCode
    var action: DescriptionEditActivity.Action = DescriptionEditActivity.Action.ADD_IMAGE_TAGS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = getString(R.string.suggested_edits_tag_images)
        imageZoomHelper = ImageZoomHelper(this)
        setStatusBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        setContentView(R.layout.activity_suggested_edits_feed_card_image_tags)
        suggestedEditsImageTagsFragment = getSupportFragmentManager().findFragmentById(R.id.imageTagFragment) as SuggestedEditsImageTagsFragment?
        addContributionButton.setOnClickListener { suggestedEditsImageTagsFragment!!.publish() }
    }

    fun updateActionButton() {
        if (suggestedEditsImageTagsFragment != null) {
            addContributionButton.setBackgroundResource(if (suggestedEditsImageTagsFragment!!.publishOutlined()) R.drawable.button_shape_border_light else R.drawable.button_shape_add_reading_list)
            addContributionText?.setTextColor(if (suggestedEditsImageTagsFragment!!.publishOutlined()) ResourceUtil.getThemedColor(this, R.attr.colorAccent) else Color.WHITE)
            addContributionButton.isEnabled = suggestedEditsImageTagsFragment!!.publishEnabled()
            addContributionButton.alpha = if (suggestedEditsImageTagsFragment!!.publishEnabled()) 1f else 0.5f
        } else {
            addContributionButton.setBackgroundResource(R.drawable.button_shape_border_light)
        }

        if (addContributionText == null) {
            // implying landscape mode, where addContributionText doesn't exist.
            addContributionImage.visibility = VISIBLE
            addContributionImage.setImageResource(R.drawable.ic_check_black_24dp)
        } else {
            addContributionText?.text = getString(R.string.description_edit_save)
            if (suggestedEditsImageTagsFragment != null) {
                addContributionText?.setTextColor(if (suggestedEditsImageTagsFragment!!.publishOutlined()) ResourceUtil.getThemedColor(this, R.attr.colorAccent) else Color.WHITE)
            } else {
                addContributionText?.setTextColor(ResourceUtil.getThemedColor(this, R.attr.colorAccent))
            }
            addContributionImage.visibility = GONE
        }
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

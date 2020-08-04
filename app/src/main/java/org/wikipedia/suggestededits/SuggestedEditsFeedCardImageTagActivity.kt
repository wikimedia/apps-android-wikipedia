package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_suggested_edits_feed_card_image_tags.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ImageZoomHelper

class SuggestedEditsFeedCardImageTagActivity : BaseActivity(), SuggestedEditsImageTagsFragment.Callback {

    private lateinit var imageZoomHelper: ImageZoomHelper
    private var suggestedEditsImageTagsFragment: SuggestedEditsImageTagsFragment? = null
    var action: DescriptionEditActivity.Action = DescriptionEditActivity.Action.ADD_IMAGE_TAGS
    var page: MwQueryPage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = Gson().fromJson(intent.getStringExtra(ARG_PAGE), MwQueryPage::class.java)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = getString(R.string.suggested_edits_tag_images)
        imageZoomHelper = ImageZoomHelper(this)
        setContentView(R.layout.activity_suggested_edits_feed_card_image_tags)
        suggestedEditsImageTagsFragment = supportFragmentManager.findFragmentById(R.id.imageTagFragment) as SuggestedEditsImageTagsFragment?
        addContributionButton.setOnClickListener { suggestedEditsImageTagsFragment!!.publish() }
        addContributionLandscapeImage.setOnClickListener { suggestedEditsImageTagsFragment!!.publish() }
    }

    override fun getLangCode(): String {
        return WikipediaApp.getInstance().language().appLanguageCode
    }

    override fun getSinglePage(): MwQueryPage? {
        return page
    }

    override fun updateActionButton() {
        if (suggestedEditsImageTagsFragment != null) {
            addContributionLandscapeImage.setBackgroundColor(ResourceUtil.getThemedColor(this, R.attr.colorAccent))
            addContributionButton.isEnabled = suggestedEditsImageTagsFragment!!.publishEnabled()
            addContributionLandscapeImage.isEnabled = suggestedEditsImageTagsFragment!!.publishEnabled()
            addContributionButton.alpha = if (suggestedEditsImageTagsFragment!!.publishEnabled()) 1f else 0.5f
            addContributionLandscapeImage.alpha = if (suggestedEditsImageTagsFragment!!.publishEnabled()) 1f else 0.5f
        }

        val isLandscape = DimenUtil.isLandscape(this)
        addContributionButton.isVisible = !isLandscape
        addContributionLandscapeImage.isVisible = isLandscape
        if (!isLandscape) {
            addContributionText?.text = getString(R.string.description_edit_save)
        }
    }

    override fun nextPage(sourceFragment: Fragment?) {
        setResult(RESULT_OK)
        finish()
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        try {
            return imageZoomHelper.onDispatchTouchEvent(event) || super.dispatchTouchEvent(event)
        } catch (e: Exception) {
        }
        return false
    }

    companion object {
        private const val ARG_PAGE = "imageTagPage"

        fun newIntent(context: Context, page: MwQueryPage): Intent {
            return Intent(context, SuggestedEditsFeedCardImageTagActivity::class.java).putExtra(ARG_PAGE, GsonMarshaller.marshal(page))
        }
    }
}

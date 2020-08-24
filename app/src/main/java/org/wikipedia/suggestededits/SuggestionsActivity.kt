package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import org.wikipedia.Constants.INTENT_EXTRA_ACTION
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.suggestededits.SuggestedEditsCardsFragment.Companion.newInstance
import org.wikipedia.views.ImageZoomHelper

class SuggestionsActivity : SingleFragmentActivity<SuggestedEditsCardsFragment>() {

    private lateinit var imageZoomHelper: ImageZoomHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = getString(getActionBarTitleRes(intent.getSerializableExtra(INTENT_EXTRA_ACTION) as Action))
        imageZoomHelper = ImageZoomHelper(this)
    }

    override fun createFragment(): SuggestedEditsCardsFragment {
        return newInstance(intent.getSerializableExtra(INTENT_EXTRA_ACTION) as Action)
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        try {
            return imageZoomHelper.onDispatchTouchEvent(event) || super.dispatchTouchEvent(event)
        } catch (e: Exception) {
        }
        return false
    }

    private fun getActionBarTitleRes(action: Action): Int {
        return if (action == ADD_IMAGE_TAGS) {
            R.string.suggested_edits_tag_images
        } else if (action == ADD_CAPTION || action == TRANSLATE_CAPTION) {
            R.string.suggested_edits_caption_images
        } else {
            R.string.suggested_edits_describe_articles
        }
    }

    companion object {
        const val EXTRA_SOURCE_ADDED_CONTRIBUTION = "addedContribution"

        @JvmStatic
        fun newIntent(context: Context, action: Action): Intent {
            return Intent(context, SuggestionsActivity::class.java).putExtra(INTENT_EXTRA_ACTION, action)
        }
    }
}

package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivitySuggestedEditsFeedCardImageTagsBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

class SuggestedEditsImageTagEditActivity : BaseActivity(), SuggestedEditsItemFragment.Callback {

    private lateinit var binding: ActivitySuggestedEditsFeedCardImageTagsBinding
    private var suggestedEditsImageTagsFragment: SuggestedEditsImageTagsFragment? = null
    var page: MwQueryPage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuggestedEditsFeedCardImageTagsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        page = Gson().fromJson(intent.getStringExtra(ARG_PAGE), MwQueryPage::class.java)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = getString(R.string.suggested_edits_tag_images)
        setImageZoomHelper()

        suggestedEditsImageTagsFragment = supportFragmentManager.findFragmentById(R.id.imageTagFragment) as SuggestedEditsImageTagsFragment?
        suggestedEditsImageTagsFragment?.invokeSource = intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource

        binding.addContributionButton.setOnClickListener { suggestedEditsImageTagsFragment!!.publish() }
        binding.addContributionLandscapeImage.setOnClickListener { suggestedEditsImageTagsFragment!!.publish() }
        maybeShowOnboarding()
    }

    override fun getLangCode(): String {
        return WikipediaApp.getInstance().language().appLanguageCode
    }

    override fun getSinglePage(): MwQueryPage? {
        return page
    }

    override fun updateActionButton() {
        if (suggestedEditsImageTagsFragment != null) {
            binding.addContributionLandscapeImage.setBackgroundColor(ResourceUtil.getThemedColor(this, R.attr.colorAccent))
            binding.addContributionButton.isEnabled = suggestedEditsImageTagsFragment!!.publishEnabled()
            binding.addContributionLandscapeImage.isEnabled = suggestedEditsImageTagsFragment!!.publishEnabled()
            binding.addContributionButton.alpha = if (suggestedEditsImageTagsFragment!!.publishEnabled()) 1f else 0.5f
            binding.addContributionLandscapeImage.alpha = if (suggestedEditsImageTagsFragment!!.publishEnabled()) 1f else 0.5f
        }

        if (DimenUtil.isLandscape(this)) {
            binding.addContributionButton.visibility = GONE
            binding.addContributionLandscapeImage.visibility = VISIBLE
        } else {
            binding.addContributionButton.visibility = VISIBLE
            binding.addContributionLandscapeImage.visibility = GONE
            binding.addContributionText.text = getString(R.string.description_edit_save)
        }
    }

    override fun nextPage(sourceFragment: Fragment?) {
        setResult(RESULT_OK)
        finish()
    }

    override fun logSuccess() {
    }

    private fun maybeShowOnboarding() {
        if (Prefs.showImageTagsOnboarding) {
            Prefs.showImageTagsOnboarding = false
            startActivity(SuggestedEditsImageTagsOnboardingActivity.newIntent(this))
        }
    }

    companion object {
        private const val ARG_PAGE = "imageTagPage"

        @JvmStatic
        fun newIntent(context: Context, page: MwQueryPage, invokeSource: Constants.InvokeSource): Intent {
            return Intent(context, SuggestedEditsImageTagEditActivity::class.java)
                    .putExtra(ARG_PAGE, JsonUtil.encodeToString(page))
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}

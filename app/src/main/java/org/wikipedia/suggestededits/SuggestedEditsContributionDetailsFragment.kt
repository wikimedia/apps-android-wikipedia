package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_contribution_diff_detail.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.Contribution.Companion.EDIT_TYPE_ARTICLE_DESCRIPTION
import org.wikipedia.suggestededits.Contribution.Companion.EDIT_TYPE_IMAGE_CAPTION
import org.wikipedia.suggestededits.SuggestedEditsContributionDetailsActivity.Companion.EXTRA_SOURCE_CONTRIBUTION
import org.wikipedia.util.DateUtil
import org.wikipedia.util.GradientUtil
import org.wikipedia.views.ViewUtil

class SuggestedEditsContributionDetailsFragment : Fragment() {
    var contribution: Contribution? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_contribution_diff_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        topView.background = GradientUtil.getPowerGradient(R.color.green90, Gravity.TOP)
        back.setOnClickListener { requireActivity().onBackPressed() }
        contribution = GsonUnmarshaller.unmarshal(Contribution::class.java, requireActivity().intent.getStringExtra(EXTRA_SOURCE_CONTRIBUTION))
        setUpContributionDetails()
    }

    private fun setUpContributionDetails() {
        contributionContainer.setOnClickListener { startTypeSpecificActivity() }
        contributionDetailText.text = contribution!!.description
        revisionText.text = contribution!!.revisionId.toString()
        contributionTitle.text = contribution!!.title
        if (contribution!!.imageUrl.isEmpty() || contribution!!.imageUrl == "null") contributionImage.visibility = GONE else ViewUtil.loadImageWithRoundedCorners(contributionImage, contribution!!.imageUrl)
        dateTimeDetailView.setLabelAndDetail(getString(R.string.suggested_edits_date_time_label), DateUtil.getFeedCardDateString(contribution!!.date) + " / " + DateUtil.get24HrFormatTimeOnlyString(contribution!!.date), -1)
        setTypSpecificData()
    }

    private fun startTypeSpecificActivity() {
        if (contribution!!.editType == EDIT_TYPE_ARTICLE_DESCRIPTION) {
            startActivity(PageActivity.newIntentForNewTab(requireActivity(), HistoryEntry(PageTitle(contribution!!.title, contribution!!.wikiSite), HistoryEntry.SOURCE_SUGGESTED_EDITS),
                    PageTitle(contribution!!.title, contribution!!.wikiSite)))
        } else {
            //Todo: Start File page activity
        }
    }

    private fun setTypSpecificData() {
        when (contribution!!.editType) {
            EDIT_TYPE_ARTICLE_DESCRIPTION -> {
                contributionCategory.text = getString(R.string.suggested_edits_article_label)
                contributionDiffText.text = getString(R.string.suggested_edits_contribution_label, contribution!!.description.length)
                pageViewsDetailView.setLabelAndDetail(getString(R.string.suggested_edits_contribution_views,
                        if (contribution!!.editType == EDIT_TYPE_ARTICLE_DESCRIPTION) getString(R.string.suggested_edits_article_label)
                        else getString(R.string.suggested_edits_image_label)), contribution!!.pageViews.toString(), R.drawable.ic_trending_up_black_24dp)
                typeDetailView.setLabelAndDetail(getString(R.string.suggested_edits_type_label), getString(R.string.description_edit_text_hint), R.drawable.ic_article_description)
                languageDetailView.setLabelAndDetail(getString(R.string.suggested_edits_language_label), WikipediaApp.getInstance().language().getAppLanguageCanonicalName(contribution!!.wikiSite.languageCode()), -1)
            }
            EDIT_TYPE_IMAGE_CAPTION -> {
                contributionCategory.text = getString(R.string.suggested_edits_image_label)
                contributionDiffText.text = getString(R.string.suggested_edits_contribution_label, contribution!!.description.length)
                pageViewsDetailView.setLabelAndDetail("", "", -1)
                typeDetailView.setLabelAndDetail(getString(R.string.suggested_edits_type_label), getString(R.string.description_edit_add_caption_hint), R.drawable.ic_image_caption)
                languageDetailView.setLabelAndDetail(getString(R.string.suggested_edits_language_label), WikipediaApp.getInstance().language().getAppLanguageCanonicalName(contribution!!.wikiSite.languageCode()), -1)
            }
            else -> {
                contributionCategory.text = getString(R.string.suggested_edits_image_label)
                contributionDiffText.text = resources.getQuantityString(R.plurals.suggested_edits_image_tag_contribution_label, contribution!!.tagCount, contribution!!.tagCount)
                typeDetailView.setLabelAndDetail(getString(R.string.suggested_edits_type_label), getString(R.string.suggested_edits_type_image_tag), R.drawable.ic_image_tag)
                pageViewsDetailView.setLabelAndDetail("", "", -1)
                languageDetailView.setLabelAndDetail("", "", -1)
            }
        }
    }

    companion object {
        fun newInstance(): SuggestedEditsContributionDetailsFragment {
            return SuggestedEditsContributionDetailsFragment()
        }
    }
}
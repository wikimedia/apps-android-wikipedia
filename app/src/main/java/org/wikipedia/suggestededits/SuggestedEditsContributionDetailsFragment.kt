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
import org.wikipedia.suggestededits.SuggestedEditsContributionDetailsActivity.Companion.EXTRA_SOURCE_CONTRIBUTION
import org.wikipedia.suggestededits.SuggestedEditsContributionsFragment.Contribution.Companion.EDIT_TYPE_ARTICLE_DESCRIPTION
import org.wikipedia.suggestededits.SuggestedEditsContributionsFragment.Contribution.Companion.EDIT_TYPE_IMAGE_CAPTION
import org.wikipedia.util.DateUtil
import org.wikipedia.util.GradientUtil
import org.wikipedia.views.ViewUtil

class SuggestedEditsContributionDetailsFragment : Fragment() {
    var contribution: SuggestedEditsContributionsFragment.Contribution? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_contribution_diff_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        topView.background = GradientUtil.getPowerGradient(R.color.green90, Gravity.TOP)
        back.setOnClickListener { requireActivity().onBackPressed() }
        contribution = GsonUnmarshaller.unmarshal(SuggestedEditsContributionsFragment.Contribution::class.java, requireActivity().intent.getStringExtra(EXTRA_SOURCE_CONTRIBUTION))
        setUpContributionDetails()
    }

    private fun setUpContributionDetails() {
        contributionContainer.setOnClickListener { startTypeSpecificActivity() }
        contributionDiffText.text = getString(R.string.suggested_edits_contribution_label, contribution!!.sizeDiff)
        contributionDetailText.text = contribution!!.description
        revisionText.text = contribution!!.revisionId.toString()

        contributionTitle.text = contribution!!.title
        if (contribution!!.imageUrl.isEmpty() || contribution!!.imageUrl == "null") contributionImage.visibility = GONE else ViewUtil.loadImageWithRoundedCorners(contributionImage, contribution!!.imageUrl)

        typeDetailView.setLabel(getString(R.string.suggested_edits_type_label))

        dateTimeDetailView.setLabel(getString(R.string.suggested_edits_date_time_label))
        dateTimeDetailView.setDetail(DateUtil.getFeedCardDateString(contribution!!.date) + " / " + DateUtil.get24HrFormatTimeOnlyString(contribution!!.date))

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
                pageViewsDetailView.setLabel(getString(R.string.suggested_edits_contribution_views, if (contribution!!.editType == EDIT_TYPE_ARTICLE_DESCRIPTION) getString(R.string.suggested_edits_article_label) else getString(R.string.suggested_edits_image_label)))
                pageViewsDetailView.setDetail(contribution!!.pageViews.toString())
                contributionIcon.setImageResource(R.drawable.ic_article_description)
                typeDetailView.setDetail(getString(R.string.description_edit_text_hint))
                languageDetailView.setLabel(getString(R.string.suggested_edits_language_label))
                languageDetailView.setDetail(WikipediaApp.getInstance().language().getAppLanguageCanonicalName(contribution!!.wikiSite.languageCode()))
            }
            EDIT_TYPE_IMAGE_CAPTION -> {
                contributionCategory.text = getString(R.string.suggested_edits_image_label)
                pageviewContainer.visibility = GONE
                pageviewDivider.visibility = GONE
                contributionIcon.setImageResource(R.drawable.ic_image_caption)
                typeDetailView.setDetail(getString(R.string.description_edit_add_caption_hint))
                languageDetailView.setLabel(getString(R.string.suggested_edits_language_label))
                languageDetailView.setDetail(WikipediaApp.getInstance().language().getAppLanguageCanonicalName(contribution!!.wikiSite.languageCode()))
            }
            else -> {
                contributionCategory.text = getString(R.string.suggested_edits_image_label)
                pageviewContainer.visibility = GONE
                pageviewDivider.visibility = GONE
                contributionIcon.setImageResource(R.drawable.ic_image_tag)
                typeDetailView.setDetail(getString(R.string.suggested_edits_type_image_tag))
                languageDetailView.visibility = GONE
                languageDetailsDivider.visibility = GONE
            }
        }
    }

    companion object {
        fun newInstance(): SuggestedEditsContributionDetailsFragment {
            return SuggestedEditsContributionDetailsFragment()
        }
    }
}
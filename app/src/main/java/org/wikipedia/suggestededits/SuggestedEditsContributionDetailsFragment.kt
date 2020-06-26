package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_contribution_diff_detail.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.Contribution.Companion.EDIT_TYPE_ARTICLE_DESCRIPTION
import org.wikipedia.suggestededits.Contribution.Companion.EDIT_TYPE_IMAGE_CAPTION
import org.wikipedia.suggestededits.Contribution.Companion.EDIT_TYPE_IMAGE_TAG
import org.wikipedia.suggestededits.SuggestedEditsContributionDetailsActivity.Companion.EXTRA_SOURCE_CONTRIBUTION
import org.wikipedia.util.DateUtil
import org.wikipedia.util.GradientUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil
import kotlin.math.abs

class SuggestedEditsContributionDetailsFragment : Fragment() {
    private lateinit var contribution: Contribution

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_contribution_diff_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        back.setOnClickListener { requireActivity().onBackPressed() }
        contribution = GsonUnmarshaller.unmarshal(Contribution::class.java, requireActivity().intent.getStringExtra(EXTRA_SOURCE_CONTRIBUTION))
        setUpContributionDetails()
    }

    private fun updateTopGradient() {
        if (contribution.sizeDiff < 0) {
            topView.background = GradientUtil.getPowerGradient(R.color.red90, Gravity.TOP)
            contributionDiffIndicatorLine.setBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.colorError))
            contributionDiffText.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.colorError))
            (requireActivity() as SuggestedEditsContributionDetailsActivity).updateStatusBarColor(ContextCompat.getColor(requireActivity(), R.color.red90))
        } else {
            topView.background = GradientUtil.getPowerGradient(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.color_group_57), Gravity.TOP)
            contributionDiffIndicatorLine.setBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.action_mode_green_background))
            contributionDiffText.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.action_mode_green_background))
            (requireActivity() as SuggestedEditsContributionDetailsActivity).updateStatusBarColor(ResourceUtil.getThemedColor(requireContext(), R.attr.color_group_57))
        }
    }

    private fun setUpContributionDetails() {
        updateTopGradient()
        contributionContainer.setOnClickListener { startTypeSpecificActivity() }
        revisionLayout.visibility = if (contribution.top) VISIBLE else GONE
        contributionTitle.text = StringUtil.removeNamespace(contribution.title)
        contributionDiffDetailText.text = contribution.description
        if (contribution.imageUrl.isNullOrEmpty() || contribution.imageUrl == "null") contributionImage.visibility = GONE else ViewUtil.loadImageWithRoundedCorners(contributionImage, contribution.imageUrl)
        dateTimeDetailView.setLabelAndDetail(getString(R.string.suggested_edits_contribution_date_time_label), DateUtil.getFeedCardDateString(contribution.date) + " / " + DateUtil.get24HrFormatTimeOnlyString(contribution.date), -1)
        setTypSpecificData()
    }

    private fun startTypeSpecificActivity() {
        if (contribution.editType == EDIT_TYPE_ARTICLE_DESCRIPTION) {
            startActivity(PageActivity.newIntentForNewTab(requireActivity(), HistoryEntry(PageTitle(contribution.title, contribution.wikiSite), HistoryEntry.SOURCE_SUGGESTED_EDITS),
                    PageTitle(contribution.title, contribution.wikiSite)))
        } else {
            startActivity(FilePageActivity.newIntent(requireContext(), PageTitle(contribution.title, contribution.wikiSite)))
        }
    }

    private fun setTypSpecificData() {
        when (contribution.editType) {
            EDIT_TYPE_ARTICLE_DESCRIPTION -> {
                contributionCategory.text = getString(R.string.suggested_edits_contribution_article_label)
                contributionDiffText.text = if (contribution.sizeDiff < 0) resources.getQuantityString(R.plurals.suggested_edits_removed_contribution_label, abs(contribution.sizeDiff), abs(contribution.sizeDiff))
                else resources.getQuantityString(R.plurals.suggested_edits_added_contribution_label, contribution.sizeDiff, contribution.sizeDiff)
                pageViewsDetailView.setLabelAndDetail(getString(R.string.suggested_edits_contribution_views,
                        if (contribution.editType == EDIT_TYPE_ARTICLE_DESCRIPTION) getString(R.string.suggested_edits_contribution_article_label)
                        else getString(R.string.suggested_edits_contribution_image_label)), contribution.pageViews.toString(), R.drawable.ic_trending_up_black_24dp)
                typeDetailView.setLabelAndDetail(getString(R.string.suggested_edits_contribution_type_label), getString(R.string.description_edit_text_hint), R.drawable.ic_article_description)
                languageDetailView.setLabelAndDetail(getString(R.string.suggested_edits_contribution_language_label), WikipediaApp.getInstance().language().getAppLanguageCanonicalName(contribution.wikiSite.languageCode()))
            }
            EDIT_TYPE_IMAGE_CAPTION -> {
                contributionCategory.text = getString(R.string.suggested_edits_contribution_image_label)
                contributionDiffText.text = if (contribution.sizeDiff < 0) resources.getQuantityString(R.plurals.suggested_edits_removed_contribution_label, abs(contribution.sizeDiff), abs(contribution.sizeDiff))
                else resources.getQuantityString(R.plurals.suggested_edits_added_contribution_label, contribution.sizeDiff, contribution.sizeDiff)
                pageViewsDetailView.setLabelAndDetail()
                typeDetailView.setLabelAndDetail(getString(R.string.suggested_edits_contribution_type_label), getString(R.string.description_edit_add_caption_hint), R.drawable.ic_image_caption)
                languageDetailView.setLabelAndDetail(getString(R.string.suggested_edits_contribution_language_label), WikipediaApp.getInstance().language().getAppLanguageCanonicalName(contribution.wikiSite.languageCode()))
            }
            EDIT_TYPE_IMAGE_TAG -> {
                contributionCategory.text = getString(R.string.suggested_edits_contribution_image_label)
                contributionDiffText.text = resources.getQuantityString(R.plurals.suggested_edits_image_tag_contribution_label, contribution.tagCount, contribution.tagCount)
                typeDetailView.setLabelAndDetail(getString(R.string.suggested_edits_contribution_type_label), getString(R.string.suggested_edits_contribution_type_image_tag), R.drawable.ic_image_tag)
                pageViewsDetailView.setLabelAndDetail()
                languageDetailView.setLabelAndDetail()
            }
            else -> {
                contributionCategory.text = getString(R.string.suggested_edits_contribution_article_label)
                contributionDiffText.text = if (contribution.sizeDiff < 0) resources.getQuantityString(R.plurals.suggested_edits_removed_contribution_label, abs(contribution.sizeDiff), abs(contribution.sizeDiff))
                else resources.getQuantityString(R.plurals.suggested_edits_added_contribution_label, contribution.sizeDiff, contribution.sizeDiff)
                typeDetailView.setLabelAndDetail(getString(R.string.suggested_edits_contribution_type_label), getString(R.string.suggested_edits_contribution_article_label), R.drawable.ic_article_description)
                pageViewsDetailView.setLabelAndDetail()
                languageDetailView.setLabelAndDetail()
            }
        }
    }

    companion object {
        fun newInstance(): SuggestedEditsContributionDetailsFragment {
            return SuggestedEditsContributionDetailsFragment()
        }
    }
}

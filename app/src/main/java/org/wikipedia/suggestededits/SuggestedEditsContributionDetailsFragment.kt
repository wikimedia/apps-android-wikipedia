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
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.suggestededits.SuggestedEditsContributionDetailsActivity.Companion.EXTRA_SOURCE_CONTRIBUTION
import org.wikipedia.suggestededits.SuggestedEditsContributionsFragment.Contribution.Companion.EDIT_TYPE_ARTICLE_DESCRIPTION
import org.wikipedia.util.DateUtil
import org.wikipedia.util.GradientUtil
import org.wikipedia.views.ViewUtil

class SuggestedEditsContributionDetailsFragment : Fragment() {
    var contribution: SuggestedEditsContributionsFragment.Contribution? = null

    companion object {
        fun newInstance(): SuggestedEditsContributionDetailsFragment {
            return SuggestedEditsContributionDetailsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_contribution_diff_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        topView.background = GradientUtil.getPowerGradient(R.color.green90, Gravity.TOP)
        contribution = GsonUnmarshaller.unmarshal(SuggestedEditsContributionsFragment.Contribution::class.java, requireActivity().intent.getStringExtra(EXTRA_SOURCE_CONTRIBUTION))
        contributionText.text = getString(R.string.suggested_edits_contribution_label, 55)
        contributionDetailText.text = contribution!!.description
        contributionCategory.text = if (contribution!!.editType == EDIT_TYPE_ARTICLE_DESCRIPTION) getString(R.string.suggested_edits_article_label) else getString(R.string.suggested_edits_image_label)
        contributionTitle.text = contribution!!.title
        contributionIcon.setImageResource(if (contribution!!.editType == EDIT_TYPE_ARTICLE_DESCRIPTION) R.drawable.ic_article_description else R.drawable.ic_image_caption)
        if (contribution!!.imageUrl.isEmpty() || contribution!!.imageUrl.equals("null")) contributionImage.visibility = GONE else ViewUtil.loadImageWithRoundedCorners(contributionImage, contribution!!.imageUrl)
        pageViewsDetailView.setLabel(getString(R.string.suggested_edits_contribution_views, if (contribution!!.editType == EDIT_TYPE_ARTICLE_DESCRIPTION) getString(R.string.suggested_edits_article_label) else getString(R.string.suggested_edits_image_label)))
        pageViewsDetailView.setDetail("599")
        typeDetailView.setLabel(getString(R.string.suggested_edits_type_label))
        typeDetailView.setDetail(if (contribution!!.editType == EDIT_TYPE_ARTICLE_DESCRIPTION) getString(R.string.description_edit_text_hint) else getString(R.string.description_edit_add_caption_hint))
        dateTimeDetailView.setLabel(getString(R.string.suggested_edits_date_time_label))
        dateTimeDetailView.setDetail(DateUtil.getFeedCardDateString(contribution!!.date) + " / " + DateUtil.get24HrFormatTimeOnlyString(contribution!!.date))
        languageDetailView.setLabel(getString(R.string.suggested_edits_language_label))
        languageDetailView.setDetail(WikipediaApp.getInstance().language().getAppLanguageCanonicalName(contribution!!.wikiSite.languageCode()))
    }
}
package org.wikipedia.suggestededits

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.google.android.material.chip.Chip
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_image_tags_item.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.suggestededits.provider.MissingDescriptionProvider
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ImageZoomHelper

class SuggestedEditsImageTagsFragment : SuggestedEditsItemFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_image_tags_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setConditionalLayoutDirection(contentContainer, parent().langFromCode)
        imageView.setLegacyVisibilityHandlingEnabled(true)
        cardItemErrorView.setBackClickListener { requireActivity().finish() }
        cardItemErrorView.setRetryClickListener {
            cardItemProgressBar.visibility = VISIBLE
            getNextItem()
        }

        val transparency = 0xcc000000
        tagsContainer.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), android.R.attr.colorBackground) and 0xffffff))

        getNextItem()
        updateContents(null)
    }

    private fun getNextItem() {
        disposables.add(MissingDescriptionProvider.getNextImageWithMissingTags(parent().langFromCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ page ->
                    updateContents(page)
                }, { this.setErrorState(it) })!!)
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        cardItemErrorView.setError(t)
        cardItemErrorView.visibility = VISIBLE
        cardItemProgressBar.visibility = GONE
        contentContainer.visibility = GONE
    }

    private fun updateContents(page: MwQueryPage?) {
        cardItemErrorView.visibility = GONE
        contentContainer.visibility = if (page != null) VISIBLE else GONE
        cardItemProgressBar.visibility = if (page != null) GONE else VISIBLE
        if (page == null) {
            return
        }

        ImageZoomHelper.setViewZoomable(imageView)

        imageView.loadImage(Uri.parse(ImageUrlUtil.getUrlForPreferredSize(page.imageInfo()!!.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)))

        tagsChipGroup.removeAllViews()
        val maxTags = 5
        for (label in page.imageLabels) {
            val chip = Chip(requireContext())
            chip.text = label.label
            chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.chip_background_color))
            chip.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.chip_text_color))
            chip.setTypeface(tagsHintText.typeface)
            tagsChipGroup.addView(chip)
            if (tagsChipGroup.childCount >= maxTags) {
                break
            }
        }
    }

    companion object {
        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsImageTagsFragment()
        }
    }
}

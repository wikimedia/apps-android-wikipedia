package org.wikipedia.commons

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_file_page.*
import kotlinx.android.synthetic.main.view_image_detail.*
import kotlinx.android.synthetic.main.view_image_detail.view.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.media.MediaHelper.getImageCaptions
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageTitle
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ImageDetailView
import org.wikipedia.views.ImageZoomHelper
import org.wikipedia.views.ViewUtil

class FilePageFragment : Fragment() {
    private lateinit var pageTitle: PageTitle
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageTitle = arguments?.getParcelable(ARG_PAGE_TITLE)!!
        retainInstance = true
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        L10nUtil.setConditionalLayoutDirection(container, pageTitle.wikiSite.languageCode())
        return inflater.inflate(R.layout.fragment_file_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as FilePageActivity).setSupportActionBar(toolbar)
        (requireActivity() as FilePageActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as FilePageActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbarTitle.text = StringUtil.removeNamespace(pageTitle.displayText)
        loadImageInfo()
    }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }

    private fun showError(caught: Throwable?) {
        progressBar.visibility = View.GONE
        detailsContainer.visibility = View.GONE
        imageView.visibility = View.GONE
        errorView.visibility = View.VISIBLE
        errorView.setError(caught)
    }

    private fun loadImageInfo() {
        disposables.add(Observable.zip(getImageCaptions(pageTitle.prefixedText),
                ServiceFactory.get(pageTitle.wikiSite).getImageInfo(pageTitle.prefixedText, pageTitle.wikiSite.languageCode()),
                BiFunction<Map<String, String>, MwQueryResponse, ImageInfo> {
                    caption: Map<String, String>, page: MwQueryResponse? ->
                    val imageInfo = page!!.query()!!.pages()!![0].imageInfo()
                    imageInfo!!.captions = caption
                    imageInfo
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    setImageDetails(it)
                }, { caught ->
                    L.e(caught)
                    showError(caught)
                }))
    }

    private fun setImageDetails(imageInfo: ImageInfo?) {
        if (imageInfo != null) {
            progressBar!!.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            ImageZoomHelper.setViewZoomable(imageView)
            ViewUtil.loadImage(imageView, imageInfo.thumbUrl)

            if (imageInfo.captions.containsKey(pageTitle.wikiSite.languageCode())) {
                addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_caption_in_language_title,
                        WikipediaApp.getInstance().language().getAppLanguageLocalizedName(pageTitle.wikiSite.languageCode())),
                        imageInfo.captions[pageTitle.wikiSite.languageCode()])
            }

            // Show the image description when a structured caption does not exist.
            addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_description_in_language_title,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(pageTitle.wikiSite.languageCode())),
                    imageInfo.metadata!!.imageDescription().trim())
            addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_artist), imageInfo.metadata!!.artist())
            addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_date), imageInfo.metadata!!.dateTime())
            addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_source), imageInfo.metadata!!.credit())
            addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_licensing), imageInfo.metadata!!.licenseShortName(), imageInfo.metadata!!.licenseUrl())
            addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_more_info),
                    getString(R.string.suggested_edits_image_preview_dialog_file_page_link_text),
                    getString(R.string.suggested_edits_image_file_page_commons_link, pageTitle.displayText))
            imageDetailsContainer.requestLayout()
        }
    }

    private fun addDetailPortion(titleString: String, detail: String?) {
        addDetailPortion(titleString, detail, null)
    }

    private fun addDetailPortion(titleString: String, detail: String?, externalLink: String?) {
        if (!detail.isNullOrEmpty()) {
            val view = ImageDetailView(requireContext())
            view.titleTextView.text = titleString
            view.detailTextView.text = StringUtil.strip(StringUtil.fromHtml(detail))
            if (!externalLink.isNullOrEmpty()) {
                view.detailTextView.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent))
                view.detailTextView.setTextIsSelectable(false)
                view.externalLinkView.visibility = View.VISIBLE
                view.detailsContainer.setOnClickListener {
                    UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(externalLink))
                }
            } else {
                view.detailTextView.movementMethod = movementMethod
            }
            imageDetailsContainer.addView(view)
        }
    }

    private val movementMethod = LinkMovementMethodExt { url: String ->
        UriUtil.handleExternalLink(context, Uri.parse(UriUtil.resolveProtocolRelativeUrl(url)))
    }

    companion object {
        private const val ARG_PAGE_TITLE = "pageTitle"
        fun newInstance(pageTitle: PageTitle): FilePageFragment {
            val fragment = FilePageFragment()
            val args = Bundle()
            args.putParcelable(ARG_PAGE_TITLE, pageTitle)
            fragment.arguments = args
            return fragment
        }
    }
}

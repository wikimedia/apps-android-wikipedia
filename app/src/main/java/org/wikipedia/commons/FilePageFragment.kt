package org.wikipedia.commons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_file_page.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.media.MediaHelper.getImageCaptions
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class FilePageFragment : Fragment() {
    private lateinit var pageTitle: PageTitle
    private lateinit var pageSummaryForEdit: PageSummaryForEdit
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
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            loadImageInfo()
        }
        errorView.setBackClickListener { requireActivity().finish() }
        loadImageInfo()
    }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }

    private fun showError(caught: Throwable?) {
        progressBar.visibility = View.GONE
        filePageView.visibility = View.GONE
        errorView.visibility = View.VISIBLE
        errorView.setError(caught)
    }

    private fun loadImageInfo() {
        lateinit var imageTags: Map<String, List<String>>
        var isFromCommons = false
        var isEditProtected = false
        var thumbnailWidth = 0
        var thumbnailHeight = 0

        errorView.visibility = View.GONE
        filePageView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        disposables.add(Observable.zip(getImageCaptions(pageTitle.prefixedText),
                ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo(pageTitle.prefixedText, pageTitle.wikiSite.languageCode()),
                BiFunction {
                    caption: Map<String, String>, response: MwQueryResponse ->
                    // set image caption to pageTitle description
                    pageTitle.description = caption[pageTitle.wikiSite.languageCode()]
                    response
                })
                .subscribeOn(Schedulers.io())
                .flatMap {
                    if (it.query()!!.pages()!![0].imageInfo() == null) {
                        // If file page originally comes from *.wikipedia.org (i.e. movie posters), it will not have imageInfo and pageId.
                        ServiceFactory.get(pageTitle.wikiSite).getImageInfo(pageTitle.prefixedText, pageTitle.wikiSite.languageCode())
                    } else {
                        // Fetch API from commons.wikimedia.org and check whether if it is not a "shared" image.
                        isFromCommons = !it.query()!!.pages()!![0].isImageShared
                        Observable.just(it)
                    }
                }
                .subscribeOn(Schedulers.io())
                .flatMap {
                    val page = it.query()!!.pages()!![0]
                    val imageInfo = page.imageInfo()!!
                    pageSummaryForEdit =  PageSummaryForEdit(
                            pageTitle.prefixedText,
                            pageTitle.wikiSite.languageCode(),
                            pageTitle,
                            pageTitle.displayText,
                            StringUtils.defaultIfBlank(StringUtil.fromHtml(imageInfo.metadata!!.imageDescription()).toString(), null),
                            imageInfo.thumbUrl,
                            null,
                            imageInfo.timestamp,
                            imageInfo.user,
                            imageInfo.metadata
                    )
                    thumbnailHeight = imageInfo.thumbHeight
                    thumbnailWidth = imageInfo.thumbWidth
                    ImageTagsProvider.getImageTagsObservable(page.pageId(), pageSummaryForEdit.lang)
                }
                .flatMap {
                    imageTags = it
                    ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getProtectionInfo(pageTitle.prefixedText)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    filePageView.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    filePageView.setup(
                            pageSummaryForEdit,
                            imageTags,
                            container.width,
                            thumbnailWidth,
                            thumbnailHeight,
                            imageFromCommons = isFromCommons,
                            showFilename = true,
                            showEditButton = isFromCommons && !isEditProtected
                    )
                }
                .subscribe({
                    isEditProtected = it.query()!!.isEditProtected
                }, { caught ->
                    L.e(caught)
                    showError(caught)
                }))
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

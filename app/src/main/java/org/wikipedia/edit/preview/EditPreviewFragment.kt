package org.wikipedia.edit.preview

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.EditFunnel
import org.wikipedia.bridge.CommunicationBridge
import org.wikipedia.bridge.CommunicationBridge.CommunicationBridgeListener
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.databinding.FragmentPreviewEditBinding
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.OkHttpWebViewClient
import org.wikipedia.edit.EditSectionActivity
import org.wikipedia.edit.summaries.EditSummaryTag
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.*
import org.wikipedia.page.references.PageReferences
import org.wikipedia.page.references.ReferenceDialog
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.views.ViewAnimations

class EditPreviewFragment : Fragment(), CommunicationBridgeListener, ReferenceDialog.Callback {

    private var _binding: FragmentPreviewEditBinding? = null
    private val binding get() = _binding!!

    private lateinit var bridge: CommunicationBridge
    private lateinit var references: PageReferences
    private lateinit var otherTag: EditSummaryTag
    private lateinit var funnel: EditFunnel
    private val summaryTags = mutableListOf<EditSummaryTag>()
    private val disposables = CompositeDisposable()
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    val isActive get() = binding.editPreviewContainer.visibility == View.VISIBLE

    override lateinit var linkHandler: LinkHandler
    override val model = PageViewModel()
    override val webView get() = binding.editPreviewWebview
    override val isPreview = true
    override val toolbarMargin = 0
    override val referencesGroup get() = references.referencesGroup
    override val selectedReferenceIndex get() = references.selectedIndex

    /**
     * Gets the overall edit summary, as specified by the user by clicking various tags,
     * and/or entering a custom summary.
     * @return Summary of the edit. If the user clicked more than one summary tag,
     * they will be separated by commas.
     */
    val summary: String
        get() {
            val summaryStr = StringBuilder()
            for (tag in summaryTags) {
                if (!tag.selected) {
                    continue
                }
                if (summaryStr.isNotEmpty()) {
                    summaryStr.append(", ")
                }
                summaryStr.append(tag)
            }
            if (otherTag.selected) {
                if (summaryStr.isNotEmpty()) {
                    summaryStr.append(", ")
                }
                summaryStr.append(otherTag)
            }
            return summaryStr.toString()
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPreviewEditBinding.inflate(layoutInflater, container, false)
        bridge = CommunicationBridge(this)
        val pageTitle = (requireActivity() as EditSectionActivity).pageTitle
        model.title = pageTitle
        model.curEntry = HistoryEntry(pageTitle, HistoryEntry.SOURCE_INTERNAL_LINK)
        funnel = WikipediaApp.instance.funnelManager.getEditFunnel(pageTitle)
        linkHandler = EditLinkHandler(requireContext())
        initWebView()

        // build up summary tags...
        val summaryTagStrings = intArrayOf(R.string.edit_summary_tag_typo,
            R.string.edit_summary_tag_grammar, R.string.edit_summary_tag_links)
        val strings = L10nUtil.getStringsForArticleLanguage(pageTitle, summaryTagStrings)

        summaryTags.clear()
        for (i in summaryTagStrings) {
            val tag = EditSummaryTag(requireActivity())
            tag.text = strings.get(i)
            tag.tag = i
            tag.setOnClickListener { view ->
                funnel.logEditSummaryTap(view.tag as Int)
                tag.isSelected = !tag.selected
            }
            binding.editSummaryTagsContainer.addView(tag)
            summaryTags.add(tag)
        }

        otherTag = EditSummaryTag(requireActivity())
        otherTag.text = L10nUtil.getStringForArticleLanguage(pageTitle, R.string.edit_summary_tag_other)
        binding.editSummaryTagsContainer.addView(otherTag)
        otherTag.setOnClickListener {
            funnel.logEditSummaryTap(R.string.edit_summary_tag_other)
            if (otherTag.selected) {
                otherTag.isSelected = false
            } else {
                (requireActivity() as EditSectionActivity).showCustomSummary()
            }
        }

        if (savedInstanceState != null) {
            for (i in summaryTags.indices) {
                summaryTags[i].isSelected = savedInstanceState.getBoolean(KEY_SUMMARY_TAG.plus(i), false)
            }
            if (savedInstanceState.containsKey(KEY_OTHER_TAG)) {
                otherTag.isSelected = true
                otherTag.text = savedInstanceState.getString(KEY_OTHER_TAG)
            }
        }
        binding.editPreviewContainer.visibility = View.GONE
        return binding.root
    }

    fun setCustomSummary(summary: String) {
        otherTag.text = summary.ifEmpty { getString(R.string.edit_summary_tag_other) }
        otherTag.isSelected = summary.isNotEmpty()
    }

    /**
     * Fetches preview html from the modified wikitext text, and shows (fades in) the Preview fragment,
     * which includes edit summary tags. When the fade-in completes, the state of the
     * actionbar button(s) is updated, and the preview is shown.
     * @param title The PageTitle associated with the text being modified.
     * @param wikiText The text of the section to be shown in the Preview.
     */
    fun showPreview(title: PageTitle, wikiText: String) {
        DeviceUtil.hideSoftKeyboard(requireActivity())
        (requireActivity() as EditSectionActivity).showProgressBar(true)
        val url = ServiceFactory.getRestBasePath(model.title!!.wikiSite) +
                RestService.PAGE_HTML_PREVIEW_ENDPOINT + UriUtil.encodeURL(title.prefixedText)
        val postData = "wikitext=" + UriUtil.encodeURL(wikiText)
        binding.editPreviewWebview.postUrl(url, postData.toByteArray())
        ViewAnimations.fadeIn(binding.editPreviewContainer) { requireActivity().invalidateOptionsMenu() }
        ViewAnimations.fadeOut(ActivityCompat.requireViewById(requireActivity(), R.id.edit_section_container))
    }

    private fun initWebView() {
        binding.editPreviewWebview.webViewClient = object : OkHttpWebViewClient() {

            override val model get() = this@EditPreviewFragment.model

            override val linkHandler get() = this@EditPreviewFragment.linkHandler

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (!isAdded) {
                    return
                }
                (requireActivity() as EditSectionActivity).showProgressBar(false)
                requireActivity().invalidateOptionsMenu()
                bridge.execute(JavaScriptActionHandler.setTopMargin(0))
            }
        }

        bridge.addListener("setup") { _, _ -> }
        bridge.addListener("final_setup") { _, _ -> }
        bridge.addListener("link", linkHandler)
        bridge.addListener("image") { _, _ -> }
        bridge.addListener("media") { _, _ -> }

        bridge.addListener("reference") { _, messagePayload ->
            (JsonUtil.decodeFromString<PageReferences>(messagePayload.toString()))?.let {
                references = it
                if (!references.referencesGroup.isNullOrEmpty()) {
                    bottomSheetPresenter.show(childFragmentManager, ReferenceDialog())
                }
            }
        }
    }

    override fun onDestroyView() {
        disposables.clear()
        binding.editPreviewWebview.clearAllListeners()
        (binding.editPreviewWebview.parent as ViewGroup).removeView(binding.editPreviewWebview)
        bridge.cleanup()
        _binding = null
        super.onDestroyView()
    }

    /**
     * Hides (fades out) the Preview fragment.
     * When fade-out completes, the state of the actionbar button(s) is updated.
     */
    fun hide(toView: View) {
        ViewAnimations.crossFade(binding.editPreviewContainer, toView) { requireActivity().invalidateOptionsMenu() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        for (i in summaryTags.indices) {
            outState.putBoolean(KEY_SUMMARY_TAG.plus(i), summaryTags[i].selected)
        }
        if (otherTag.selected) {
            outState.putString(KEY_OTHER_TAG, otherTag.toString())
        }
    }

    inner class EditLinkHandler constructor(context: Context) : LinkHandler(context) {
        override fun onPageLinkClicked(anchor: String, linkText: String) {
            // TODO: also need to handle references, issues, disambig, ... in preview eventually
        }

        override fun onInternalLinkClicked(title: PageTitle) {
            showLeavingEditDialogue {
                startActivity(
                    PageActivity.newIntentForCurrentTab(
                        context,
                        HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK), title
                    )
                )
            }
        }

        override fun onExternalLinkClicked(uri: Uri) {
            showLeavingEditDialogue { UriUtil.handleExternalLink(context, uri) }
        }

        override fun onMediaLinkClicked(title: PageTitle) {
            // ignore
        }

        override fun onDiffLinkClicked(title: PageTitle, revisionId: Long) {
            // ignore
        }

        /**
         * Shows the user a dialogue asking them if they really meant to leave the edit
         * workflow, and warning them that their changes have not yet been saved.
         *
         * @param runnable The runnable that is run if the user chooses to leave.
         */
        private fun showLeavingEditDialogue(runnable: Runnable) {
            // Ask the user if they really meant to leave the edit workflow
            val leavingEditDialog = AlertDialog.Builder(requireActivity())
                .setMessage(R.string.dialog_message_leaving_edit)
                .setPositiveButton(R.string.dialog_message_leaving_edit_leave) { dialog, _: Int ->
                    // They meant to leave; close dialogue and run specified action
                    dialog.dismiss()
                    runnable.run()
                }
                .setNegativeButton(R.string.dialog_message_leaving_edit_stay, null)
                .create()
            leavingEditDialog.show()
        }

        @Suppress("UNUSED_PARAMETER")
        override var wikiSite: WikiSite
            get() = model.title!!.wikiSite
            set(wikiSite) {}
    }

    companion object {
        private const val KEY_OTHER_TAG = "otherTag"
        private const val KEY_SUMMARY_TAG = "summaryTag"
    }
}

package org.wikipedia.edit.preview

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.bridge.CommunicationBridge
import org.wikipedia.bridge.CommunicationBridge.CommunicationBridgeListener
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.databinding.FragmentPreviewEditBinding
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.OkHttpWebViewClient
import org.wikipedia.edit.EditSectionActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.*
import org.wikipedia.page.references.PageReferences
import org.wikipedia.page.references.ReferenceDialog
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.UriUtil

class EditPreviewFragment : Fragment(), CommunicationBridgeListener, ReferenceDialog.Callback {

    private var _binding: FragmentPreviewEditBinding? = null
    private val binding get() = _binding!!

    private lateinit var bridge: CommunicationBridge
    private lateinit var references: PageReferences
    val isActive get() = binding.editPreviewContainer.visibility == View.VISIBLE

    override lateinit var linkHandler: LinkHandler
    override val model = PageViewModel()
    override val webView get() = binding.editPreviewWebview
    override val isPreview = true
    override val toolbarMargin = 0
    override val referencesGroup get() = references.referencesGroup
    override val selectedReferenceIndex get() = references.selectedIndex

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPreviewEditBinding.inflate(layoutInflater, container, false)
        bridge = CommunicationBridge(this)
        val pageTitle = (requireActivity() as EditSectionActivity).pageTitle
        model.title = pageTitle
        model.curEntry = HistoryEntry(pageTitle, HistoryEntry.SOURCE_INTERNAL_LINK)
        linkHandler = EditLinkHandler(requireContext())
        initWebView()

        binding.editPreviewContainer.visibility = View.GONE
        return binding.root
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
        ActivityCompat.requireViewById<View>(requireActivity(), R.id.edit_section_container).isVisible = false
        binding.editPreviewContainer.isVisible = true
        requireActivity().invalidateOptionsMenu()
    }

    private fun initWebView() {
        webView.setBackgroundColor(ResourceUtil.getThemedColor(requireActivity(), R.attr.paper_color))
        binding.editPreviewWebview.webViewClient = object : OkHttpWebViewClient() {

            override val model get() = this@EditPreviewFragment.model

            override val linkHandler get() = this@EditPreviewFragment.linkHandler

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (!isAdded) {
                    return
                }
                bridge.onMetadataReady()
                bridge.execute(JavaScriptActionHandler.setMargins(16, 0, 16, 16 + DimenUtil.roundedPxToDp(binding.licenseText.height.toFloat())))
                (requireActivity() as EditSectionActivity).showProgressBar(false)
                requireActivity().invalidateOptionsMenu()
            }
        }

        bridge.addListener("setup") { _, _ -> }
        bridge.addListener("final_setup") { _, _ ->
            if (isAdded) {
                bridge.onPcsReady()
            }
        }
        bridge.addListener("link", linkHandler)
        bridge.addListener("image") { _, _ -> }
        bridge.addListener("media") { _, _ -> }

        bridge.addListener("reference") { _, messagePayload ->
            (JsonUtil.decodeFromString<PageReferences>(messagePayload.toString()))?.let {
                references = it
                if (references.referencesGroup.isNotEmpty()) {
                    ExclusiveBottomSheetPresenter.show(childFragmentManager, ReferenceDialog())
                }
            }
        }
    }

    override fun onDestroyView() {
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
        binding.editPreviewContainer.isVisible = false
        toView.isVisible = true
        requireActivity().invalidateOptionsMenu()
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
            MaterialAlertDialogBuilder(requireActivity())
                .setMessage(R.string.dialog_message_leaving_edit)
                .setPositiveButton(R.string.dialog_message_leaving_edit_leave) { dialog, _: Int ->
                    // They meant to leave; close dialogue and run specified action
                    dialog.dismiss()
                    runnable.run()
                }
                .setNegativeButton(R.string.dialog_message_leaving_edit_stay, null)
                .show()
        }

        @Suppress("UNUSED_PARAMETER")
        override var wikiSite: WikiSite
            get() = model.title!!.wikiSite
            set(wikiSite) {}
    }
}

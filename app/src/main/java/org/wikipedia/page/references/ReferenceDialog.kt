package org.wikipedia.page.references

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import org.jsoup.Jsoup
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.databinding.FragmentReferencesPagerBinding
import org.wikipedia.databinding.ViewReferencePagerItemBinding
import org.wikipedia.dataclient.okhttp.OkHttpWebViewClient
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.PageViewModel
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import java.util.Locale

class ReferenceDialog : ExtendedBottomSheetDialogFragment() {
    interface Callback {
        val linkHandler: LinkHandler
        val referencesGroup: List<PageReferences.Reference>?
        val selectedReferenceIndex: Int
    }

    private var _binding: FragmentReferencesPagerBinding? = null
    private val binding get() = _binding!!
    private val blankModel = PageViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReferencesPagerBinding.inflate(inflater, container, false)
        callback()?.let {
            it.referencesGroup?.run {
                binding.referenceTitleText.text = requireContext().getString(R.string.reference_title, "")
                binding.referencePager.offscreenPageLimit = 2
                binding.referencePager.adapter = ReferencesAdapter(this)
                TabLayoutMediator(binding.pageIndicatorView, binding.referencePager) { _, _ -> }.attach()
                binding.referencePager.setCurrentItem(it.selectedReferenceIndex, true)
                L10nUtil.setConditionalLayoutDirection(binding.root, it.linkHandler.wikiSite.languageCode)
            } ?: return@let null
        } ?: run {
            dismiss()
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (callback()?.referencesGroup?.size == 1) {
            binding.pageIndicatorView.visibility = View.GONE
            binding.indicatorDivider.visibility = View.GONE
        }
        BottomSheetBehavior.from(binding.root.parent as View).peekHeight = DimenUtil.displayHeightPx
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun processLinkTextWithAlphaReferences(linkText: String): String {
        var newLinkText = linkText
        val isLowercase = newLinkText.contains("lower")
        if (newLinkText.contains("alpha ")) {
            val strings = newLinkText.split(" ")
            var alphaReference = StringUtil.getBase26String(strings.last().replace("]", "").toInt())
            alphaReference = if (isLowercase) alphaReference.lowercase(Locale.getDefault()) else alphaReference
            newLinkText = alphaReference
        }
        return newLinkText.replace("[\\[\\]]".toRegex(), "") + "."
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : BottomSheetDialog(requireActivity(), theme) {
            override fun onBackPressed() {
                if (binding.referencePager.currentItem > 0) {
                    binding.referencePager.setCurrentItem(binding.referencePager.currentItem - 1, true)
                } else {
                    super.onBackPressed()
                }
            }
        }
    }

    private inner class ViewHolder(val binding: ViewReferencePagerItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindItem(idText: CharSequence?, reference: PageReferences.Reference) {
            binding.referenceId.text = idText
            binding.root.post {
                if (isAdded) {
                    val contents = reference.html
                    if (contents.isEmpty()) {
                        // Inspect html for links without anchor text
                        val links = Jsoup.parse(reference.html).select("a[href]").map { it.attr("href") }
                        var tags = ""
                        for (i in links.indices) {
                            tags = tags.plus("<a href='${links[i]}'>[${i + 1}]</a>")
                        }
                        setContent(tags)
                        binding.referenceExtLink.isVisible = tags.isNotEmpty()
                        return@post
                    }
                    binding.referenceExtLink.isVisible = false
                    setContent(contents)
                }
            }
        }

        private fun setContent(html: String) {
            val wikiSite = callback()?.linkHandler?.wikiSite ?: WikipediaApp.instance.wikiSite
            val colorHex = ResourceUtil.colorToCssString(
                ResourceUtil.getThemedColor(
                    requireContext(),
                    android.R.attr.textColorPrimary
                )
            )
            val dir = if (L10nUtil.isLangRTL(wikiSite.languageCode)) "rtl" else "ltr"
            binding.referenceTextWebView.setBackgroundColor(Color.TRANSPARENT)
            binding.referenceTextWebView.webViewClient = object : OkHttpWebViewClient() {
                override val model get() = blankModel
                override val linkHandler get() = callback()?.linkHandler!!
                override val linkHandlerOverride get() = true
            }
            binding.referenceTextWebView.loadDataWithBaseURL(
                wikiSite.uri.buildUpon().toString(),
                "${JavaScriptActionHandler.getCssStyles(wikiSite)}<div style=\"line-height: 150%; color: #$colorHex\" dir=\"$dir\">$html</div>",
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    private inner class ReferencesAdapter(val references: List<PageReferences.Reference>) : RecyclerView.Adapter<ViewHolder>() {
        override fun getItemCount(): Int {
            return references.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ViewReferencePagerItemBinding.inflate(LayoutInflater.from(context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindItem(processLinkTextWithAlphaReferences(references[position].text), references[position])
        }
    }

    private fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }
}

package org.wikipedia.page.references

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.databinding.FragmentReferencesPagerBinding
import org.wikipedia.databinding.ViewReferencePagerItemBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import java.util.*

class ReferenceDialog : ExtendedBottomSheetDialogFragment() {
    interface Callback {
        val linkHandler: LinkHandler
        val referencesGroup: List<PageReferences.Reference>?
        val selectedReferenceIndex: Int
    }

    private var _binding: FragmentReferencesPagerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReferencesPagerBinding.inflate(inflater, container, false)
        callback()?.let {
            it.referencesGroup?.run {
                binding.referenceTitleText.text = requireContext().getString(R.string.reference_title, "")
                binding.referencePager.offscreenPageLimit = 2
                binding.referencePager.adapter = ReferencesAdapter(this)
                TabLayoutMediator(binding.pageIndicatorView, binding.referencePager) { _, _ -> }.attach()
                binding.referencePager.setCurrentItem(it.selectedReferenceIndex, true)
                L10nUtil.setConditionalLayoutDirection(binding.root, it.linkHandler.wikiSite.languageCode())
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
        } else {
            val behavior = BottomSheetBehavior.from(binding.root.parent as View)
            behavior.setPeekHeight(DimenUtil.displayHeightPx / 2)
        }
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
            var alphaReference = StringUtil.getBase26String(strings[strings.size - 1].replace("]", "").toInt())
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

    private inner class ViewHolder constructor(val binding: ViewReferencePagerItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.referenceText.movementMethod = LinkMovementMethodExt(callback()?.linkHandler)
        }

        fun bindItem(idText: CharSequence?, contents: CharSequence?) {
            binding.referenceId.text = idText
            binding.referenceText.text = contents
        }
    }

    private inner class ReferencesAdapter constructor(val references: List<PageReferences.Reference>) : RecyclerView.Adapter<ViewHolder>() {
        override fun getItemCount(): Int {
            return references.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ViewReferencePagerItemBinding.inflate(LayoutInflater.from(context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindItem(processLinkTextWithAlphaReferences(references[position].text),
                    StringUtil.fromHtml(StringUtil.removeCiteMarkup(StringUtil.removeStyleTags(references[position].content))))
        }
    }

    private fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }
}

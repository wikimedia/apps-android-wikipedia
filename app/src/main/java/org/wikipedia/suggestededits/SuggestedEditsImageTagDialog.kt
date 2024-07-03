package org.wikipedia.suggestededits

import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.os.Bundle
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.databinding.DialogImageTagSelectBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L

class SuggestedEditsImageTagDialog : DialogFragment() {
    interface Callback {
        fun onSearchSelect(item: ImageTag)
        fun onSearchDismiss(searchTerm: String)
    }

    private lateinit var textWatcher: TextWatcher
    private var _binding: DialogImageTagSelectBinding? = null
    private val binding get() = _binding!!
    private var currentSearchTerm: String = ""
    private val adapter = ResultListAdapter(emptyList())
    private var searchJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogImageTagSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.imageTagsRecycler.layoutManager = LinearLayoutManager(activity)
        binding.imageTagsRecycler.adapter = adapter
        textWatcher = binding.imageTagsSearchText.doOnTextChanged { text, _, _, _ ->
            currentSearchTerm = text?.toString() ?: ""
            requestResults(currentSearchTerm)
        }
        applyResults(emptyList())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        val model = ShapeAppearanceModel.builder().setAllCornerSizes(DimenUtil.dpToPx(6f)).build()
        val materialShapeDrawable = MaterialShapeDrawable(model)
        materialShapeDrawable.fillColor = ResourceUtil.getThemedColorStateList(requireActivity(), R.attr.background_color)
        materialShapeDrawable.elevation = dialog.window!!.decorView.elevation

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val inset = DimenUtil.roundedDpToPx(16f)
            val insetDrawable = InsetDrawable(materialShapeDrawable, inset, inset, inset, inset)
            dialog.window!!.setBackgroundDrawable(insetDrawable)
        } else {
            dialog.window!!.setBackgroundDrawable(materialShapeDrawable)
        }

        val params = dialog.window!!.attributes
        params.gravity = Gravity.TOP
        dialog.window!!.attributes = params

        return dialog
    }

    override fun onStart() {
        super.onStart()
        try {
            if (requireArguments().getBoolean("useClipboardText")) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                if (clipboard.hasPrimaryClip() && clipboard.primaryClip != null) {
                    val primaryClip = clipboard.primaryClip!!
                    val clipText = primaryClip.getItemAt(primaryClip.itemCount - 1).coerceToText(requireContext()).toString()
                    if (clipText.isNotEmpty()) {
                        binding.imageTagsSearchText.setText(clipText)
                        binding.imageTagsSearchText.selectAll()
                    }
                }
            } else if (requireArguments().getString("lastText")!!.isNotEmpty()) {
                binding.imageTagsSearchText.setText(requireArguments().getString("lastText")!!)
                binding.imageTagsSearchText.selectAll()
            }
        } catch (ignore: Exception) {
        }
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        binding.imageTagsSearchText.removeTextChangedListener(textWatcher)
        _binding = null
    }

    private fun requestResults(searchTerm: String) {
        if (searchTerm.isEmpty()) {
            applyResults(emptyList())
            return
        }

        searchJob?.cancel()
        searchJob = lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.d(throwable)
        }) {
            delay(500)
            val search = ServiceFactory.get(Constants.wikidataWikiSite).searchEntities(searchTerm, WikipediaApp.instance.appOrSystemLanguageCode, WikipediaApp.instance.appOrSystemLanguageCode)
            val labelList = search.results.map { ImageTag(it.id, it.label, it.description) }
            applyResults(labelList)
        }
    }

    private fun applyResults(results: List<ImageTag>) {
        adapter.setResults(results)
        adapter.notifyDataSetChanged()
        if (currentSearchTerm.isEmpty()) {
            binding.noResultsText.visibility = View.GONE
            binding.imageTagsRecycler.visibility = View.GONE
            binding.imageTagsDivider.visibility = View.INVISIBLE
        } else {
            binding.imageTagsDivider.visibility = View.VISIBLE
            if (results.isEmpty()) {
                binding.noResultsText.visibility = View.VISIBLE
                binding.imageTagsRecycler.visibility = View.GONE
            } else {
                binding.noResultsText.visibility = View.GONE
                binding.imageTagsRecycler.visibility = View.VISIBLE
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback()?.onSearchDismiss(currentSearchTerm)
    }

    private inner class ResultItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        fun bindItem(item: ImageTag) {
            itemView.findViewById<TextView>(R.id.labelName).text = item.label
            itemView.findViewById<TextView>(R.id.labelDescription).text = item.description
            itemView.tag = item
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val item = v.tag as ImageTag
            callback()?.onSearchSelect(item)
            dismiss()
        }
    }

    private inner class ResultListAdapter(private var results: List<ImageTag>) : RecyclerView.Adapter<ResultItemHolder>() {
        fun setResults(results: List<ImageTag>) {
            this.results = results
        }

        override fun getItemCount(): Int {
            return results.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): ResultItemHolder {
            val view = layoutInflater.inflate(R.layout.item_wikidata_label, parent, false)
            val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            view.layoutParams = params
            return ResultItemHolder(view)
        }

        override fun onBindViewHolder(holder: ResultItemHolder, pos: Int) {
            holder.bindItem(results[pos])
        }
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        fun newInstance(useClipboardText: Boolean, lastText: String): SuggestedEditsImageTagDialog {
            val dialog = SuggestedEditsImageTagDialog()
            dialog.arguments = bundleOf("useClipboardText" to useClipboardText, "lastText" to lastText)
            return dialog
        }
    }
}

package org.wikipedia.suggestededits

import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.os.Bundle
import android.text.TextWatcher
import android.view.*
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.databinding.DialogImageTagSelectBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.wikidata.Search
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import kotlin.collections.ArrayList

class SuggestedEditsImageTagDialog : DialogFragment() {
    interface Callback {
        fun onSearchSelect(item: MwQueryPage.ImageLabel)
        fun onSearchDismiss(searchTerm: String)
    }

    private lateinit var textWatcher: TextWatcher
    private var _binding: DialogImageTagSelectBinding? = null
    private val binding get() = _binding!!
    private var currentSearchTerm: String = ""
    private val adapter = ResultListAdapter(emptyList())
    private val disposables = CompositeDisposable()

    private val searchRunnable = Runnable {
        if (isAdded) {
            requestResults(currentSearchTerm)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogImageTagSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.imageTagsRecycler.layoutManager = LinearLayoutManager(activity)
        binding.imageTagsRecycler.adapter = adapter
        textWatcher = binding.imageTagsSearchText.doOnTextChanged { text, _, _, _ ->
            currentSearchTerm = text?.toString() ?: ""
            binding.imageTagsSearchText.removeCallbacks(searchRunnable)
            binding.imageTagsSearchText.postDelayed(searchRunnable, 500)
        }
        applyResults(emptyList())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        val surfaceColor = ResourceUtil.getThemedColor(requireActivity(), R.attr.searchItemBackground)
        val model = ShapeAppearanceModel.builder().setAllCornerSizes(DimenUtil.dpToPx(6f)).build()
        val materialShapeDrawable = MaterialShapeDrawable(model)
        materialShapeDrawable.fillColor = ColorStateList.valueOf(surfaceColor)
        materialShapeDrawable.elevation = ViewCompat.getElevation(dialog.window!!.decorView)

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
        binding.imageTagsSearchText.removeTextChangedListener(textWatcher)
        binding.imageTagsSearchText.removeCallbacks(searchRunnable)
        disposables.clear()
        _binding = null
    }

    private fun requestResults(searchTerm: String) {
        if (searchTerm.isEmpty()) {
            applyResults(emptyList())
            return
        }
        disposables.add(ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).searchEntities(searchTerm, WikipediaApp.getInstance().appOrSystemLanguageCode, WikipediaApp.getInstance().appOrSystemLanguageCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ search: Search ->
                    val labelList = ArrayList<MwQueryPage.ImageLabel>()
                    for (result in search.results()) {
                        val label = MwQueryPage.ImageLabel(result.id, result.label, result.description)
                        labelList.add(label)
                    }
                    applyResults(labelList)
                }) { t: Throwable? ->
                    L.d(t)
                })
    }

    private fun applyResults(results: List<MwQueryPage.ImageLabel>) {
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

    private inner class ResultItemHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        fun bindItem(item: MwQueryPage.ImageLabel) {
            itemView.findViewById<TextView>(R.id.labelName).text = item.label
            itemView.findViewById<TextView>(R.id.labelDescription).text = item.description
            itemView.tag = item
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val item = v!!.tag as MwQueryPage.ImageLabel
            callback()?.onSearchSelect(item)
            dismiss()
        }
    }

    private inner class ResultListAdapter(private var results: List<MwQueryPage.ImageLabel>) : RecyclerView.Adapter<ResultItemHolder>() {
        fun setResults(results: List<MwQueryPage.ImageLabel>) {
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

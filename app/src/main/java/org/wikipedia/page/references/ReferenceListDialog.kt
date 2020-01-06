package org.wikipedia.page.references

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_reference_list.*
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration

class ReferenceListDialog : ExtendedBottomSheetDialogFragment() {
    interface Callback {
        val references: Observable<References>
    }

    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_reference_list, container)
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(view!!.parent as View).peekHeight = DimenUtil
                .roundedDpToPx(DimenUtil.getDimension(R.dimen.readingListSheetPeekHeight))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        referencesRecycler.layoutManager = LinearLayoutManager(activity)
        referencesRecycler.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable, false, false))
        updateList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposables.clear()
    }

    private fun updateList() {
        disposables.add(callback()!!.references
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ references: References -> referencesRecycler.adapter = ReferenceListAdapter(references) }) { t: Throwable? -> L.d(t) })
    }

    private inner class ReferenceListItemHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val referenceTextView: TextView
        fun bindItem(reference: References.Reference?) {
            referenceTextView.text = StringUtil.fromHtml(reference!!.content)
        }

        init {
            referenceTextView = itemView.findViewById(R.id.reference_text)
        }
    }

    private inner class ReferenceListAdapter(private val references: References) : RecyclerView.Adapter<ReferenceListItemHolder>() {
        private val referenceKeys: Array<String> = references.referencesMap.keys.toTypedArray()

        override fun getItemCount(): Int {
            return referenceKeys.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): ReferenceListItemHolder {
            val view = layoutInflater.inflate(R.layout.item_reference, null)
            return ReferenceListItemHolder(view)
        }

        override fun onBindViewHolder(holder: ReferenceListItemHolder, pos: Int) {
            holder.bindItem(references.referencesMap[referenceKeys[pos]])
        }
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        fun newInstance(): ReferenceListDialog {
            return ReferenceListDialog()
        }
    }
}
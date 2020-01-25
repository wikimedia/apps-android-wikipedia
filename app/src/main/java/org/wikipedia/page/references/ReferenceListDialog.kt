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
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration

class ReferenceListDialog : ExtendedBottomSheetDialogFragment() {
    interface Callback {
        val references: Observable<References>
        val linkHandler: LinkHandler
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
        L10nUtil.setConditionalLayoutDirection(view, callback()!!.linkHandler.wikiSite.languageCode())
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

    private inner class ReferenceListItemHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val referenceTextView: TextView = itemView.findViewById(R.id.reference_text)
        private val referenceIdView: TextView = itemView.findViewById(R.id.reference_id_text)
        private val referenceBackLink: View = itemView.findViewById(R.id.reference_back_link)
        init {
            referenceTextView.movementMethod = LinkMovementMethodExt(callback()!!.linkHandler)
            referenceBackLink.setOnClickListener(this)
        }

        fun bindItem(reference: References.Reference, position: Int) {
            referenceIdView.text = ((position + 1).toString() + ".")
            referenceTextView.text = StringUtil.fromHtml(StringUtil.removeCiteMarkup(StringUtil.removeStyleTags(reference.content)))
            if (reference.backLinks.isEmpty()) {
                referenceBackLink.visibility = View.GONE
            } else {
                referenceBackLink.visibility = View.VISIBLE
                referenceBackLink.tag = reference.backLinks[0].href
            }
        }

        override fun onClick(v: View?) {
            val href = v!!.tag as String
            callback()!!.linkHandler.onPageLinkClicked(href, "")
        }
    }

    private inner class ReferenceListAdapter(private val references: References) : RecyclerView.Adapter<ReferenceListItemHolder>() {
        private val referenceKeys: Array<String> = references.referencesMap.keys.toTypedArray()

        override fun getItemCount(): Int {
            return referenceKeys.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): ReferenceListItemHolder {
            val view = layoutInflater.inflate(R.layout.item_reference, null)
            val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            view.layoutParams = params
            return ReferenceListItemHolder(view)
        }

        override fun onBindViewHolder(holder: ReferenceListItemHolder, pos: Int) {
            holder.bindItem(references.referencesMap[referenceKeys[pos]]!!, pos)
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
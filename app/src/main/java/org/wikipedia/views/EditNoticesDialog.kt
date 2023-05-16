package org.wikipedia.views

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.databinding.DialogEditNoticesBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L

@SuppressLint("ViewConstructor")
class EditNoticesDialog constructor(
        val wikiSite: WikiSite,
        val editNotices: List<String>,
        activity: Activity
) : MaterialAlertDialogBuilder(activity) {

    private val binding = DialogEditNoticesBinding.inflate(activity.layoutInflater)
    private var dialog: AlertDialog? = null

    private val movementMethod = LinkMovementMethodExt { urlStr ->
        L.v("Link clicked was $urlStr")
        UriUtil.visitInExternalBrowser(context, Uri.parse(UriUtil.resolveProtocolRelativeUrl(wikiSite, urlStr)))
    }

    init {
        setView(binding.root)
        binding.editNoticeCloseButton.setOnClickListener { dialog?.dismiss() }

        binding.editNoticeRecycler.adapter = EditNoticesAdapter()
        binding.editNoticeRecycler.layoutManager = LinearLayoutManager(context)
        binding.editNoticeRecycler.addItemDecoration(DrawableItemDecoration(context, R.attr.list_divider, drawStart = true, drawEnd = true))

        binding.editNoticeCheckbox.isChecked = Prefs.autoShowEditNotices
        binding.editNoticeCheckbox.setOnCheckedChangeListener { _, isChecked -> Prefs.autoShowEditNotices = isChecked }
    }

    override fun show(): AlertDialog {
        dialog = super.show()
        return dialog!!
    }

    private inner class EditNoticesAdapter : RecyclerView.Adapter<DefaultViewHolder>() {
        override fun getItemCount(): Int {
            return editNotices.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder {
            val textView = TextView(context)
            textView.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            textView.movementMethod = movementMethod
            textView.setPadding(0, DimenUtil.roundedDpToPx(16f), 0, DimenUtil.roundedDpToPx(16f))
            return DefaultViewHolder(textView)
        }

        override fun onBindViewHolder(holder: DefaultViewHolder, pos: Int) {
            holder.bindItem(pos)
        }
    }

    private open inner class DefaultViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        open fun bindItem(position: Int) {
            (itemView as TextView).text = StringUtil.fromHtml(StringUtil.removeStyleTags(editNotices[position]))
        }
    }
}

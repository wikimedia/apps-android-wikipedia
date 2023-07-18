package org.wikipedia.page.issues

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.databinding.DialogPageIssuesBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.DrawableItemDecoration

class PageIssuesDialog constructor(
    activity: Activity,
    val wikiSite: WikiSite,
    val pageIssues: List<String>,
    val callback: Callback
) : MaterialAlertDialogBuilder(activity) {

    fun interface Callback {
        fun onLinkClicked(url: String, title: String?, linkText: String)
    }

    private val binding = DialogPageIssuesBinding.inflate(activity.layoutInflater)
    private var dialog: AlertDialog? = null

    private val movementMethod = LinkMovementMethodExt { url, title, linkText ->
        callback.onLinkClicked(url, title, linkText)
        dialog?.dismiss()
    }

    init {
        setView(binding.root)

        binding.recycler.adapter = EditNoticesAdapter()
        binding.recycler.layoutManager = LinearLayoutManager(context)
        binding.recycler.addItemDecoration(DrawableItemDecoration(context, R.attr.list_divider, drawStart = true, drawEnd = true))
    }

    override fun show(): AlertDialog {
        dialog = super.show()
        return dialog!!
    }

    private inner class EditNoticesAdapter : RecyclerView.Adapter<DefaultViewHolder>() {
        override fun getItemCount(): Int {
            return pageIssues.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder {
            val textView = TextView(context)
            textView.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            textView.setTextColor(ResourceUtil.getThemedColor(context, R.attr.primary_color))
            textView.setLineSpacing(DimenUtil.dpToPx(6f), 1f)
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
            (itemView as TextView).text = StringUtil.fromHtml(StringUtil.removeStyleTags(pageIssues[position]))
        }
    }
}

package org.wikipedia.views

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.databinding.DialogEditNoticesBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L

@SuppressLint("ViewConstructor")
class EditNoticesDialog constructor(
        val wikiSite: WikiSite,
        val editNotices: List<String>,
        context: Context
) : AlertDialog(context) {

    private val binding = DialogEditNoticesBinding.inflate(LayoutInflater.from(context))

    private val movementMethod = LinkMovementMethodExt { urlStr ->
        L.v("Link clicked was $urlStr")
        UriUtil.visitInExternalBrowser(context, Uri.parse(UriUtil.resolveProtocolRelativeUrl(wikiSite, urlStr)))
        /*
        var url = UriUtil.resolveProtocolRelativeUrl(urlStr)
        if (url.startsWith("/wiki/")) {
            val title = pageTitle.wikiSite.titleForInternalLink(url)
            startActivity(PageActivity.newIntentForCurrentTab(this, HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK), title))
        } else {
            val uri = Uri.parse(url)
            val authority = uri.authority
            if (authority != null && WikiSite.supportedAuthority(authority) &&
                    uri.path != null && uri.path!!.startsWith("/wiki/")) {
                val title = WikiSite(uri).titleForUri(uri)
                startActivity(PageActivity.newIntentForCurrentTab(this, HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK), title))
            } else {
                // if it's a /w/ URI, turn it into a full URI and go external
                if (url.startsWith("/w/")) {
                    url = String.format("%1\$s://%2\$s", pageTitle.wikiSite.scheme(),
                            pageTitle.wikiSite.authority()) + url
                }
                UriUtil.handleExternalLink(this, Uri.parse(url))
            }
        }
        */
    }

    init {
        setView(binding.root)
        setTitle(R.string.edit_notices)
        setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok)) { _, _ -> }

        binding.editNoticesRecycler.adapter = EditNoticesAdapter()
        binding.editNoticesRecycler.layoutManager = LinearLayoutManager(context)
        binding.editNoticesRecycler.addItemDecoration(DrawableItemDecoration(context, R.attr.list_separator_drawable, drawStart = true, drawEnd = true))

        binding.editNoticesCheckbox.isChecked = true
        binding.editNoticesCheckbox.setOnClickListener {
            // TODO
        }
    }

    private inner class EditNoticesAdapter : RecyclerView.Adapter<DefaultViewHolder>() {
        override fun getItemCount(): Int {
            return editNotices.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder {
            val textView = TextView(context)
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
            (itemView as TextView).text = StringUtil.fromHtml(editNotices[position])
        }
    }
}

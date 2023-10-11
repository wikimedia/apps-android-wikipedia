package org.wikipedia.diff

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.dataclient.restbase.DiffResponse
import org.wikipedia.dataclient.restbase.Revision
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

object DiffUtil {

    fun buildDiffLinesList(context: Context, diffList: List<DiffResponse.DiffItem>): List<DiffLine> {
        val items = mutableListOf<DiffLine>()
        var lastItem: DiffLine? = null
        diffList.forEach {
            val item = DiffLine(context, it)
            // coalesce diff lines that occur on successive line numbers
            if (lastItem != null &&
                    ((item.diff.lineNumber - lastItem!!.diff.lineNumber == 1 && lastItem!!.diff.type == DiffResponse.DIFF_TYPE_LINE_ADDED && item.diff.type == DiffResponse.DIFF_TYPE_LINE_ADDED) ||
                            (item.diff.lineNumber - lastItem!!.diff.lineNumber == 1 && lastItem!!.diff.type == DiffResponse.DIFF_TYPE_LINE_WITH_SAME_CONTENT && item.diff.type == DiffResponse.DIFF_TYPE_LINE_WITH_SAME_CONTENT) ||
                            (lastItem!!.diff.type == DiffResponse.DIFF_TYPE_LINE_REMOVED && item.diff.type == DiffResponse.DIFF_TYPE_LINE_REMOVED))) {
                if (it.lineNumber > lastItem!!.lineEnd) {
                    lastItem!!.lineEnd = it.lineNumber
                }
                val str = SpannableStringBuilder(lastItem!!.parsedText)
                str.append("\n")
                str.append(item.parsedText)
                lastItem!!.parsedText = str
            } else {
                items.add(item)
                lastItem = item
            }
        }
        return items
    }

    fun buildDiffLinesList(context: Context, singleRev: Revision): List<DiffLine> {
        val range = DiffResponse.HighlightRange(0, 0, DiffResponse.HIGHLIGHT_TYPE_ADD)
        val item = DiffResponse.DiffItem(DiffResponse.DIFF_TYPE_LINE_ADDED, 1, singleRev.source, null, listOf(range))
        return listOf(DiffLine(context, item))
    }

    class DiffLine(context: Context, item: DiffResponse.DiffItem) {
        val diff = item
        val lineStart = item.lineNumber
        var lineEnd = item.lineNumber
        var parsedText = createSpannableDiffText(context, diff)
        var expanded = diff.type != DiffResponse.DIFF_TYPE_LINE_WITH_SAME_CONTENT
    }

    class DiffLinesAdapter(private val diffLines: List<DiffLine>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount(): Int {
            return diffLines.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return DiffLineHolder(DiffLineView(parent.context))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            if (holder is DiffLineHolder) {
                holder.bindItem(diffLines[pos])
            }
            holder.itemView.tag = pos
        }
    }

    private class DiffLineHolder constructor(itemView: DiffLineView) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(item: DiffLine) {
            (itemView as DiffLineView).setItem(item)
        }
    }

    private fun createSpannableDiffText(context: Context, diff: DiffResponse.DiffItem): CharSequence {
        val spannableString = SpannableStringBuilder(diff.text.ifEmpty { "\n" })
        if (diff.text.isEmpty()) {
            spannableString.setSpan(EmptyLineSpan(ResourceUtil.getThemedColor(context, android.R.attr.colorBackground),
                    ResourceUtil.getThemedColor(context, R.attr.placeholder_color)), 0, spannableString.length, 0)
            return spannableString
        }
        when (diff.type) {
            DiffResponse.DIFF_TYPE_LINE_ADDED -> {
                updateDiffTextDecor(context, spannableString, true, 0, diff.text.length)
            }
            DiffResponse.DIFF_TYPE_LINE_REMOVED -> {
                updateDiffTextDecor(context, spannableString, false, 0, diff.text.length)
            }
            DiffResponse.DIFF_TYPE_PARAGRAPH_MOVED_FROM -> {
                updateDiffTextDecor(context, spannableString, false, 0, diff.text.length)
            }
            DiffResponse.DIFF_TYPE_PARAGRAPH_MOVED_TO -> {
                updateDiffTextDecor(context, spannableString, true, 0, diff.text.length)
            }
        }
        if (diff.highlightRanges.isNotEmpty()) {
            for (highlightRange in diff.highlightRanges) {
                val indices = StringUtil.utf8Indices(diff.text)
                val highlightRangeStart = indices[highlightRange.start].coerceIn(0, diff.text.length)
                val highlightRangeEnd = (indices.getOrElse(highlightRange.start + highlightRange.length) { indices.last() + 1 }).coerceIn(0, diff.text.length)

                if (highlightRange.type == DiffResponse.HIGHLIGHT_TYPE_ADD) {
                    updateDiffTextDecor(context, spannableString, true, highlightRangeStart, highlightRangeEnd)
                } else {
                    updateDiffTextDecor(context, spannableString, false, highlightRangeStart, highlightRangeEnd)
                }
            }
        }
        return spannableString
    }

    private fun updateDiffTextDecor(context: Context, spannableText: SpannableStringBuilder, isAddition: Boolean, start: Int, end: Int) {
        val boldStyle = StyleSpan(Typeface.BOLD)
        val foregroundAddedColor = ForegroundColorSpan(ResourceUtil.getThemedColor(context, R.attr.primary_color))
        val foregroundRemovedColor = ForegroundColorSpan(ResourceUtil.getThemedColor(context, R.attr.primary_color))
        spannableText.setSpan(BackgroundColorSpan(ColorUtils.setAlphaComponent(ResourceUtil.getThemedColor(context,
                if (isAddition) R.attr.success_color else R.attr.destructive_color), 48)), start, end, 0)
        spannableText.setSpan(boldStyle, start, end, 0)
        if (isAddition) {
            spannableText.setSpan(foregroundAddedColor, start, end, 0)
        } else {
            spannableText.setSpan(foregroundRemovedColor, start, end, 0)
            spannableText.setSpan(StrikethroughSpan(), start, end, 0)
        }
    }
}

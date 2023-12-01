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
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.text.set
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
                lastItem!!.parsedText = buildSpannedString {
                    appendLine(lastItem!!.parsedText)
                    append(item.parsedText)
                }
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
        return buildSpannedString {
            if (diff.text.isEmpty()) {
                inSpans(EmptyLineSpan(context)) {
                    appendLine()
                }
            } else {
                append(diff.text)

                when (diff.type) {
                    DiffResponse.DIFF_TYPE_LINE_ADDED, DiffResponse.DIFF_TYPE_PARAGRAPH_MOVED_TO -> {
                        updateDiffTextDecor(context, true, 0, diff.text.length)
                    }
                    DiffResponse.DIFF_TYPE_LINE_REMOVED, DiffResponse.DIFF_TYPE_PARAGRAPH_MOVED_FROM -> {
                        updateDiffTextDecor(context, false, 0, diff.text.length)
                    }
                }

                for (highlightRange in diff.highlightRanges) {
                    val indices = StringUtil.utf8Indices(diff.text)
                    val highlightRangeStart = indices[highlightRange.start].coerceIn(0, diff.text.length)
                    val highlightRangeEnd = (indices.getOrElse(highlightRange.start + highlightRange.length) { indices.last() + 1 }).coerceIn(0, diff.text.length)
                    val isAddition = highlightRange.type == DiffResponse.HIGHLIGHT_TYPE_ADD

                    updateDiffTextDecor(context, isAddition, highlightRangeStart, highlightRangeEnd)
                }
            }
        }
    }

    private fun SpannableStringBuilder.updateDiffTextDecor(context: Context, isAddition: Boolean, start: Int, end: Int) {
        this[start, end] = BackgroundColorSpan(ColorUtils.setAlphaComponent(ResourceUtil.getThemedColor(context,
            if (isAddition) R.attr.success_color else R.attr.destructive_color), 48))
        this[start, end] = StyleSpan(Typeface.BOLD)
        this[start, end] = ForegroundColorSpan(ResourceUtil.getThemedColor(context, R.attr.primary_color))
        if (!isAddition) {
            this[start, end] = StrikethroughSpan()
        }
    }
}

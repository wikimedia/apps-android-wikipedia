package org.wikipedia.edit

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputConnection
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import org.wikipedia.R
import org.wikipedia.databinding.ViewWikitextKeyboardBinding
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil

class WikiTextKeyboardView constructor(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    interface Callback {
        fun onPreviewLink(title: String)
        fun onRequestInsertMedia()
        fun onRequestInsertLink()
        fun onRequestHeading()
        fun onRequestFormatting()
        fun onSyntaxOverlayCollapse()
    }

    private val binding = ViewWikitextKeyboardBinding.inflate(LayoutInflater.from(context), this)
    var callback: Callback? = null
    var editText: SyntaxHighlightableEditText? = null

    var userMentionVisible: Boolean
        get() { return binding.wikitextButtonUserMention.isVisible }
        set(value) { binding.wikitextButtonUserMention.isVisible = value }

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.WikitextKeyboardView) {
                val headingsEnable = getBoolean(R.styleable.WikitextKeyboardView_headingsEnable, true)
                binding.wikitextButtonHeading.isVisible = headingsEnable
            }
        }

        binding.wikitextButtonUndo.visibility = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) VISIBLE else GONE
        binding.wikitextButtonRedo.visibility = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) VISIBLE else GONE
        binding.wikitextButtonTextFormat.setExpandable(true)
        binding.wikitextButtonHeading.setExpandable(true)

        binding.wikitextButtonLink.setOnClickListener {
            editText?.inputConnection?.let {
                if (it.getSelectedText(0).isNullOrEmpty()) {
                    callback?.onRequestInsertLink()
                } else {
                    toggleSyntaxAroundCurrentSelection(editText, it, "[[", "]]")
                }
            }
        }

        binding.wikitextButtonTextFormat.setOnClickListener {
            callback?.onRequestFormatting()
        }

        binding.wikitextButtonHeading.setOnClickListener {
            callback?.onRequestHeading()
        }

        binding.wikitextButtonTemplate.setOnClickListener {
            editText?.inputConnection?.let {
                toggleSyntaxAroundCurrentSelection(editText, it, "{{", "}}")
            }
        }

        binding.wikitextButtonRef.setOnClickListener {
            editText?.inputConnection?.let {
                toggleSyntaxAroundCurrentSelection(editText, it, "<ref>", "</ref>")
            }
        }

        binding.wikitextButtonListBulleted.setOnClickListener {
            editText?.inputConnection?.commitText("\n* ", 1)
        }

        binding.wikitextButtonListNumbered.setOnClickListener {
            editText?.inputConnection?.commitText("\n# ", 1)
        }

        binding.wikitextButtonInsertMedia.setOnClickListener {
            callback?.onRequestInsertMedia()
        }

        binding.wikitextButtonPreviewLink.setOnClickListener {
            editText?.inputConnection?.let { inputConnection ->
                var title: String? = null
                val selection = inputConnection.getSelectedText(0)?.toString()
                if (!selection.isNullOrEmpty() && "[[" !in selection) {
                    title = selection.trim('.', ',', ';', '?', '!')
                } else {
                    val (before, after) = if (selection != null && selection.length > 1) {
                        selection.substring(0, selection.length / 2) to
                                selection.substring(selection.length / 2)
                    } else {
                        val peekLength = 64
                        inputConnection.getTextBeforeCursor(peekLength, 0)?.toString() to
                                inputConnection.getTextAfterCursor(peekLength, 0)?.toString()
                    }

                    if (!before.isNullOrEmpty() && !after.isNullOrEmpty()) {
                        val str = before + after
                        val i1 = before.lastIndexOf("[[")
                        if (i1 >= 0) {
                            title = str.substring(i1 + 2).substringBefore("]]")
                                .trim()
                                .splitToSequence('|')
                                .firstOrNull()?.ifEmpty { null }
                        }
                    }
                }

                title?.let {
                    callback?.onPreviewLink(it)
                }
            }
        }

        binding.wikitextButtonUndo.setOnClickListener {
            editText?.undo()
        }

        binding.wikitextButtonRedo.setOnClickListener {
            editText?.redo()
        }

        binding.wikitextButtonUserMention.setOnClickListener {
            editText?.inputConnection?.commitText("@", 1)
        }
    }

    fun insertLink(title: PageTitle, baseLangCode: String) {
        editText?.inputConnection?.let {
            var text = "[["
            if (title.wikiSite.languageCode != baseLangCode) {
                text += ":" + title.wikiSite.languageCode + ":"
            }
            val displayText = StringUtil.fromHtml(title.displayText).toString()
            text += if (StringUtil.removeUnderscores(title.prefixedText) != displayText) {
                title.prefixedText + "|" + displayText + "]]"
            } else {
                "$displayText]]"
            }
            editText?.inputConnection?.commitText(text, 1)
        }
    }

    fun onAfterFormattingShown() {
        binding.wikitextButtonHeading.setExpanded(false)
        binding.wikitextButtonTextFormat.setExpanded(true)
    }

    fun onAfterHeadingsShown() {
        binding.wikitextButtonTextFormat.setExpanded(false)
        binding.wikitextButtonHeading.setExpanded(true)
    }

    fun onAfterOverlaysHidden() {
        binding.wikitextButtonTextFormat.setExpanded(false)
        binding.wikitextButtonHeading.setExpanded(false)
    }

    companion object {
        fun toggleSyntaxAroundCurrentSelection(editText: EditText?, ic: InputConnection, prefix: String, suffix: String) {
            editText?.let {
                if (it.selectionStart == it.selectionEnd) {
                    val before = ic.getTextBeforeCursor(prefix.length, 0)
                    val after = ic.getTextAfterCursor(suffix.length, 0)
                    if (before != null && before.toString() == prefix && after != null && after.toString() == suffix) {
                        // the cursor is actually inside the exact syntax, so negate it.
                        ic.deleteSurroundingText(prefix.length, suffix.length)
                    } else {
                        // nothing highlighted, so just insert link syntax, and place the cursor in the center.
                        ic.commitText(prefix + suffix, 1)
                        ic.commitText("", -suffix.length)
                    }
                } else {
                    var selection = ic.getSelectedText(0) ?: return
                    selection = if (selection.toString().startsWith(prefix) && selection.toString().endsWith(suffix)) {
                        // the highlighted text is already a link, so toggle the link away.
                        selection.subSequence(prefix.length, selection.length - suffix.length)
                    } else {
                        // put link syntax around the highlighted text.
                        prefix + selection + suffix
                    }
                    ic.commitText(selection, 1)
                    ic.setSelection(it.selectionStart - selection.length, it.selectionEnd)
                }
            }
        }
    }
}

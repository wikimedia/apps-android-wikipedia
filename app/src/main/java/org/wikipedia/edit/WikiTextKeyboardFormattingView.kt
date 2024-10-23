package org.wikipedia.edit

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.wikipedia.databinding.ViewWikitextKeyboardFormattingBinding
import org.wikipedia.util.FeedbackUtil

class WikiTextKeyboardFormattingView : FrameLayout {
    private val binding = ViewWikitextKeyboardFormattingBinding.inflate(LayoutInflater.from(context), this)
    var callback: WikiTextKeyboardView.Callback? = null
    var editText: SyntaxHighlightableEditText? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        binding.closeButton.setOnClickListener {
            callback?.onSyntaxOverlayCollapse()
        }
        FeedbackUtil.setButtonTooltip(binding.closeButton)
        binding.wikitextButtonBold.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "'''", "'''")
            }
        }
        binding.wikitextButtonItalic.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "''", "''")
            }
        }
        binding.wikitextButtonUnderline.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "<u>", "</u>")
            }
        }
        binding.wikitextButtonStrikethrough.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "<s>", "</s>")
            }
        }
        binding.wikitextButtonSup.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "<sup>", "</sup>")
            }
        }
        binding.wikitextButtonSub.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "<sub>", "</sub>")
            }
        }
        binding.wikitextButtonTextLarge.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "<big>", "</big>")
            }
        }
        binding.wikitextButtonTextSmall.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "<small>", "</small>")
            }
        }
        binding.wikitextButtonCode.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "<code>", "</code>")
            }
        }
    }
}

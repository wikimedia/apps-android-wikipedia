package org.wikipedia.edit

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.wikipedia.databinding.ViewWikitextKeyboardFormattingBinding

class WikiTextKeyboardFormattingView : FrameLayout {
    private val binding = ViewWikitextKeyboardFormattingBinding.inflate(LayoutInflater.from(context), this)
    var callback: WikiTextKeyboardView.Callback? = null
    var editText: SyntaxHighlightableEditText? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        binding.wikitextButtonBold.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "'''", "'''")
                callback?.onSyntaxOverlayClicked()
            }
        }
        binding.wikitextButtonItalic.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "''", "''")
                callback?.onSyntaxOverlayClicked()
            }
        }
        binding.wikitextButtonUnderline.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "<u>", "</u>")
                callback?.onSyntaxOverlayClicked()
            }
        }
        binding.wikitextButtonStrikethrough.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "<s>", "</s>")
                callback?.onSyntaxOverlayClicked()
            }
        }
        binding.wikitextButtonSup.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "<sup>", "</sup>")
                callback?.onSyntaxOverlayClicked()
            }
        }
        binding.wikitextButtonSub.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "<sub>", "</sub>")
                callback?.onSyntaxOverlayClicked()
            }
        }
        binding.wikitextButtonTextLarge.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "<big>", "</big>")
                callback?.onSyntaxOverlayClicked()
            }
        }
        binding.wikitextButtonTextSmall.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "<small>", "</small>")
                callback?.onSyntaxOverlayClicked()
            }
        }
    }
}

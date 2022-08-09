package org.wikipedia.edit

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.wikipedia.databinding.ViewWikitextKeyboardHeadingsBinding
import org.wikipedia.views.SyntaxHighlightableEditText

class WikiTextKeyboardHeadingsView : FrameLayout {
    private val binding = ViewWikitextKeyboardHeadingsBinding.inflate(LayoutInflater.from(context), this)
    var callback: WikiTextKeyboardView.Callback? = null
    var editText: SyntaxHighlightableEditText? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        binding.wikitextButtonH2.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "==", "==")
                callback?.onSyntaxOverlayClicked()
            }
        }
        binding.wikitextButtonH3.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "===", "===")
                callback?.onSyntaxOverlayClicked()
            }
        }
        binding.wikitextButtonH4.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "====", "====")
                callback?.onSyntaxOverlayClicked()
            }
        }
        binding.wikitextButtonH5.setOnClickListener {
            editText?.inputConnection?.let {
                WikiTextKeyboardView.toggleSyntaxAroundCurrentSelection(editText, it, "=====", "=====")
                callback?.onSyntaxOverlayClicked()
            }
        }
    }
}

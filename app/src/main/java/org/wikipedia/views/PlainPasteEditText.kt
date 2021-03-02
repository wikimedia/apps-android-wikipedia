package org.wikipedia.views

import android.content.ClipboardManager
import android.content.Context
import android.os.SystemClock
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.MenuRes
import androidx.core.content.getSystemService
import com.google.android.material.textfield.TextInputEditText
import org.wikipedia.edit.richtext.SpanExtents
import org.wikipedia.edit.richtext.SyntaxHighlighter
import org.wikipedia.edit.richtext.SyntaxHighlighter.OnSyntaxHighlightListener
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.log.L.w
import java.util.*

class PlainPasteEditText : TextInputEditText {
    interface FindListener {
        fun onFinished(activeMatchOrdinal: Int, numberOfMatches: Int, textPosition: Int, findingNext: Boolean)
    }

    private val findInPageTextPositionList: MutableList<Int> = ArrayList()
    private var findInPageCurrentTextPosition = 0
    private var syntaxHighlighter: SyntaxHighlighter? = null
    var inputConnection: InputConnection? = null
    var findListener: FindListener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun onTextContextMenuItem(id: Int): Boolean {
        return if (id == android.R.id.paste) {
            onTextContextMenuPaste(id)
        } else super.onTextContextMenuItem(id)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        inputConnection = super.onCreateInputConnection(outAttrs)

        // For multiline EditTexts that specify a done keyboard action, unset the no carriage return
        // flag which otherwise limits the EditText to a single line
        val multilineInput = inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE == InputType.TYPE_TEXT_FLAG_MULTI_LINE
        val actionDone = outAttrs.imeOptions and EditorInfo.IME_ACTION_DONE == EditorInfo.IME_ACTION_DONE
        if (actionDone && multilineInput) {
            outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION.inv()
        }
        return inputConnection
    }

    fun undo() {
        inputConnection?.let {
            it.sendKeyEvent(KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON))
            it.sendKeyEvent(KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON))
        }
    }

    fun redo() {
        inputConnection?.let {
            it.sendKeyEvent(KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON))
            it.sendKeyEvent(KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON))
        }
    }

    private fun onTextContextMenuPaste(@MenuRes menuId: Int): Boolean {
        // Do not allow pasting of formatted text!
        // We do this by intercepting the clipboard and temporarily replacing its
        // contents with plain text.
        val clipboard = context.getSystemService<ClipboardManager>()!!
        if (clipboard.hasPrimaryClip() && clipboard.primaryClip != null) {
            clipboard.primaryClip?.let {
                val lastClipText = it.getItemAt(it.itemCount - 1).coerceToText(context).toString()
                // temporarily set the new clip data as the primary
                ClipboardUtil.setPlainText(context, null, lastClipText)
                // execute the paste!
                super.onTextContextMenuItem(menuId)
                // restore the clip data back to the old one.
                try {
                    clipboard.setPrimaryClip(it)
                } catch (e: Exception) {
                    // This could be a FileUriExposedException, among others, where we are unable to
                    // set the clipboard contents back to their original state. Unfortunately there's
                    // nothing to be done in that case.
                    w(e)
                }
            }
        }
        return true
    }

    fun findInEditor(targetText: String?, syntaxHighlighter: SyntaxHighlighter) {
        findListener?.let { listener ->
            this.syntaxHighlighter = syntaxHighlighter
            findInPageCurrentTextPosition = 0
            // apply find text syntax
            syntaxHighlighter.applyFindTextSyntax(targetText, object : OnSyntaxHighlightListener {
                override fun syntaxHighlightResults(spanExtents: List<SpanExtents>) { }

                override fun findTextMatches(spanExtents: List<SpanExtents>) {
                    findInPageTextPositionList.clear()
                    findInPageTextPositionList.addAll(spanExtents.map { it.start })
                    onFinished(false, listener)
                }
            })
        }
    }

    fun findNext() {
        find(true)
    }

    fun findPrevious() {
        find(false)
    }

    fun findFirstOrLast(isFirst: Boolean) {
        findListener?.let {
            findInPageCurrentTextPosition = if (isFirst) 0 else findInPageTextPositionList.size - 1
            onFinished(true, it)
            syntaxHighlighter!!.setSelectedMatchResultPosition(findInPageCurrentTextPosition)
        }
    }

    private fun find(isNext: Boolean) {
        findListener?.let {
            findInPageCurrentTextPosition = if (isNext) {
                if (findInPageCurrentTextPosition == findInPageTextPositionList.size - 1) 0 else ++findInPageCurrentTextPosition
            } else {
                if (findInPageCurrentTextPosition == 0) findInPageTextPositionList.size - 1 else --findInPageCurrentTextPosition
            }
            onFinished(true, it)
            syntaxHighlighter!!.setSelectedMatchResultPosition(findInPageCurrentTextPosition)
        }
    }

    fun clearMatches(syntaxHighlighter: SyntaxHighlighter) {
        findInEditor(null, syntaxHighlighter)
    }

    private fun onFinished(findingNext: Boolean, listener: FindListener) {
        listener.onFinished(findInPageCurrentTextPosition,
                findInPageTextPositionList.size,
                if (findInPageTextPositionList.isEmpty()) 0 else findInPageTextPositionList[findInPageCurrentTextPosition],
                findingNext)
    }
}

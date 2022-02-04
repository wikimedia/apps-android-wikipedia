package org.wikipedia.views

import android.content.ClipData
import android.content.Context
import android.os.SystemClock
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.view.ContentInfoCompat
import androidx.core.view.ViewCompat
import com.google.android.material.textfield.TextInputEditText
import org.wikipedia.edit.richtext.SpanExtents
import org.wikipedia.edit.richtext.SyntaxHighlighter
import org.wikipedia.edit.richtext.SyntaxHighlighter.OnSyntaxHighlightListener
import java.util.*

open class PlainPasteEditText : TextInputEditText {
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

    init {
        // The MIME type(s) need to be set for onReceiveContent() to be called.
        ViewCompat.setOnReceiveContentListener(this, arrayOf("text/*"), null)
    }

    override fun onReceiveContent(payload: ContentInfoCompat): ContentInfoCompat? {
        // Do not allow pasting of formatted text! We do this by replacing the contents of the clip
        // with plain text.
        val clip = payload.clip
        val lastClipText = clip.getItemAt(clip.itemCount - 1).coerceToText(context).toString()
        val updatedPayload = ContentInfoCompat.Builder(payload)
            .setClip(ClipData.newPlainText(null, lastClipText))
            .build()
        return super.onReceiveContent(updatedPayload)
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

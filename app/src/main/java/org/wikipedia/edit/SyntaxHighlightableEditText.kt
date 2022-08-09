package org.wikipedia.edit

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.text.InputType
import android.text.TextPaint
import android.util.AttributeSet
import android.view.ContentInfo
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import androidx.core.view.ViewCompat
import org.wikipedia.R
import org.wikipedia.edit.richtext.SpanExtents
import org.wikipedia.edit.richtext.SyntaxHighlighter
import org.wikipedia.edit.richtext.SyntaxHighlighter.OnSyntaxHighlightListener
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import java.util.*

@SuppressLint("AppCompatCustomView")
open class SyntaxHighlightableEditText : EditText {
    interface FindListener {
        fun onFinished(activeMatchOrdinal: Int, numberOfMatches: Int, textPosition: Int, findingNext: Boolean)
    }

    private val findInPageTextPositionList: MutableList<Int> = ArrayList()
    private var findInPageCurrentTextPosition = 0
    private var syntaxHighlighter: SyntaxHighlighter? = null
    private var prevLineCount = -1
    private var prevLineHeight = 0
    private val lineNumberHelper = LineNumberHelper()
    private val lineNumberPaint = TextPaint()

    lateinit var scrollView: View
    var inputConnection: InputConnection? = null
    var findListener: FindListener? = null
    var allowScrollToCursor = true

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        // The MIME type(s) need to be set for onReceiveContent() to be called.
        ViewCompat.setOnReceiveContentListener(this, arrayOf("text/*"), null)

        lineNumberPaint.isAntiAlias = true
        lineNumberPaint.textAlign = Paint.Align.RIGHT
        lineNumberPaint.textSize = this.textSize * 0.8f
        lineNumberPaint.color = ResourceUtil.getThemedColor(context, R.attr.material_theme_de_emphasised_color)
    }

    override fun bringPointIntoView(offset: Int): Boolean {
        if (!allowScrollToCursor) {
            return false
        }
        return super.bringPointIntoView(offset)
    }

    override fun onDraw(canvas: Canvas?) {
        if (prevLineCount != lineCount) {
            prevLineHeight = lineHeight
            prevLineCount = lineCount
            lineNumberHelper.computeLines(0, prevLineCount, layout, text.toString())
        }

        // if showLineNumbers
        if (true && layout != null) {
            val wrapContent = true // TODO: make wrap content optional?

            val firstLine = layout.getLineForVertical(scrollView.scrollY)
            val lastLine = layout.getLineForVertical(scrollView.scrollY + scrollView.height)
            var curX = layout.getLineTop(firstLine) + prevLineHeight

            var prevNum = -1
            for (i in firstLine..lastLine) {
                val num = lineNumberHelper.realLines[i]
                if (!wrapContent || prevNum != num) {
                    prevNum = num
                    canvas?.drawText(num.toString(),
                            DimenUtil.dpToPx(24f),
                            curX.toFloat(),
                            lineNumberPaint)
                }
                curX += prevLineHeight
            }
        }
        super.onDraw(canvas)
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

    override fun onReceiveContent(payload: ContentInfo): ContentInfo? {
        var newPayload = payload
        try {
            // Do not allow pasting of formatted text! We do this by replacing the contents of the clip
            // with plain text.
            val clip = payload.clip
            val lastClipText = clip.getItemAt(clip.itemCount - 1).coerceToText(context).toString()

            newPayload = ContentInfo.Builder(payload)
                    .setClip(ClipData.newPlainText(null, lastClipText))
                    .build()
        } catch (e: Exception) {
            L.e(e)
        }
        return super.onReceiveContent(newPayload)
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
                    text?.let {
                        findInPageTextPositionList.addAll(spanExtents.map { span -> it.getSpanStart(span) })
                    }
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

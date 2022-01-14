package org.wikipedia.views

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.widget.doOnTextChanged
import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import java.util.*

class UserMentionEditText : PlainPasteEditText {
    interface Listener {
        fun onStartUserNameEntry()
        fun onCancelUserNameEntry()
        fun onUserNameChanged(userName: String)
    }

    var listener: Listener? = null

    private val textWatcher: TextWatcher?
    private var userNameStartPos = -1
    private var userNameEndPos = -1
    private val isEnteringUserName get() = userNameStartPos >= 0
    private var isUserNameCommitting = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        textWatcher = doOnTextChanged { text, start, before, count ->
            if (text == null) {
                return@doOnTextChanged
            }
            if (count == 1 && start < text.length && text[start] == '@' &&
                    (start == 0 || (start > 0 && text[start - 1] == ' ')) &&
                    !isEnteringUserName) {
                userNameStartPos = start
                userNameEndPos = userNameStartPos
                onStartUserNameEntry()
            }

            if (isEnteringUserName) {
                if (count - before == 1 && start + count - 1 < text.length && start + count - 1 >= 0 &&
                        text[start + count - 1] == ' ') {
                    onCancelUserNameEntry()
                } else {
                    userNameEndPos += (count - before)
                }
                if (userNameEndPos <= userNameStartPos) {
                    onCancelUserNameEntry()
                }
            }
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (textWatcher == null || isUserNameCommitting) {
            return
        }
        if (isEnteringUserName) {
            if (selStart < userNameStartPos || selEnd > userNameEndPos) {
                onCancelUserNameEntry()
                return
            }
            onUserNameChanged(text!!.substring(userNameStartPos, userNameEndPos))
        } else if (selStart == selEnd && !isEnteringUserName) {
            val spans = text!!.getSpans(selStart, selEnd, UserColorSpan::class.java)
            if (spans.isNotEmpty()) {
                userNameStartPos = text!!.getSpanStart(spans[0])
                userNameEndPos = text!!.getSpanEnd(spans[0])
                onStartUserNameEntry()
                onUserNameChanged(text!!.substring(userNameStartPos, userNameEndPos))
            }
        }
    }

    private fun onStartUserNameEntry() {
        listener?.onStartUserNameEntry()
    }

    private fun onCancelUserNameEntry() {
        userNameStartPos = -1
        userNameEndPos = -1
        listener?.onCancelUserNameEntry()
    }

    private fun onUserNameChanged(userName: String) {
        L.d("Entering username: $userName")
        listener?.onUserNameChanged(userName)
    }

    fun onCommitUserName(userName: String) {
        try {
            isUserNameCommitting = true
            if (userNameStartPos < 0 || userNameEndPos <= userNameStartPos) {
                onCancelUserNameEntry()
                return
            }

            val sb = SpannableStringBuilder()
            sb.append(text!!.subSequence(0, userNameStartPos))
            val spanStart = sb.length
            sb.append("@$userName")
            val spanEnd = sb.length
            if (userNameEndPos < text!!.length) {
                sb.append(text!!.subSequence(userNameEndPos, text!!.length - 1))
            }

            val span = UserColorSpan(ResourceUtil.getThemedColor(context, R.attr.colorAccent))
            sb.setSpan(span, spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            text = sb
            setSelection(spanEnd)
            onCancelUserNameEntry()
        } finally {
            isUserNameCommitting = false
        }
    }

    fun getParsedText(wikiSite: WikiSite): String {
        if (text == null) {
            return ""
        }
        var str = text!!.toString()

        val spans = text!!.getSpans(0, text!!.length, UserColorSpan::class.java)
        if (spans.isNotEmpty()) {

            val pairs = mutableListOf<MutablePair<Int, Int>>()
            spans.forEach {
                pairs.add(MutablePair(text!!.getSpanStart(it), text!!.getSpanEnd(it)))
            }
            pairs.sortBy { it.first }

            for (i in 0 until pairs.size) {
                var name = str.substring(pairs[i].first, pairs[i].second)
                if (name.length > 1 && name.startsWith("@")) {
                    name = name.substring(1)
                }
                name = "[[" + UserAliasData.valueFor(wikiSite.languageCode) + ":" + name + "|@" + name + "]]"
                str = str.replaceRange(pairs[i].first, pairs[i].second, name)

                val lenDiff = name.length - (pairs[i].second - pairs[i].first)
                for (j in i + 1 until pairs.size) {
                    pairs[j].first += lenDiff
                    pairs[j].second += lenDiff
                }
            }
        }
        return str
    }

    data class MutablePair<T, U>(var first: T, var second: U)

    private class UserColorSpan(@ColorInt foreColor: Int) : ForegroundColorSpan(foreColor)
}

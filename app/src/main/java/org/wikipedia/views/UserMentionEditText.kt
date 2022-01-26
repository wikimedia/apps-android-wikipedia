package org.wikipedia.views

import android.content.Context
import android.text.*
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
    private var spacesPressedCount = 0
    private var isUserNameCommitting = false
    private val editable get() = text ?: SpannableStringBuilder("")

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {

        // This is a state machine that behaves roughly as follows:
        // * When the "@" symbol is typed, it enters a state of inputting a username.
        // * During the username input state, every subsequent keypress will send an event to the
        //   listener, to give it a chance to show a dropdown selection for searching usernames.
        // * If the user selects a final username from the dropdown selection, the input state is
        //   finished, and the username is turned into a special Span that contains the username at
        //   that position.
        // * If a space " " character is pressed, without having selected a username from the
        //   dropdown list, the input state is cancelled, and no special Span is added.
        // * For all other keypresses, while not in the username input state, it behaves as usual.
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
                val spacePressed = count - before == 1 && start + count - 1 < text.length && start + count - 1 >= 0 &&
                        text[start + count - 1] == ' '
                if (spacePressed) {
                    spacesPressedCount++
                }

                if (spacePressed && spacesPressedCount > 1) {
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
            if ((selStart < userNameStartPos || selEnd > userNameEndPos) ||
                    (userNameEndPos > editable.length)) {
                onCancelUserNameEntry()
                return
            }
            onUserNameChanged(editable.substring(userNameStartPos, userNameEndPos))
        } else if (selStart == selEnd && !isEnteringUserName) {
            val spans = editable.getSpans(selStart, selEnd, UserColorSpan::class.java)
            if (spans.isNotEmpty()) {
                userNameStartPos = editable.getSpanStart(spans[0])
                userNameEndPos = editable.getSpanEnd(spans[0])
                onStartUserNameEntry()
                onUserNameChanged(editable.substring(userNameStartPos, userNameEndPos))
            }
        }
    }

    private fun onStartUserNameEntry() {
        listener?.onStartUserNameEntry()
        spacesPressedCount = 0
    }

    private fun onCancelUserNameEntry() {
        userNameStartPos = -1
        userNameEndPos = -1
        spacesPressedCount = 0
        listener?.onCancelUserNameEntry()
    }

    private fun onUserNameChanged(userName: String) {
        L.d("Entering username: $userName")
        listener?.onUserNameChanged(userName)
    }

    fun prepopulateUserName(userName: String) {
        val sb = SpannableStringBuilder()
        sb.append("@$userName")
        val spanEnd = sb.length
        sb.append(" ")
        createUserNameSpan(sb, 0, spanEnd)
        isUserNameCommitting = true
        text = sb
        isUserNameCommitting = false
        setSelection(sb.length)
    }

    fun onCommitUserName(userName: String) {
        try {
            isUserNameCommitting = true
            if (userNameStartPos < 0 || userNameEndPos <= userNameStartPos) {
                onCancelUserNameEntry()
                return
            }

            val sb = SpannableStringBuilder()
            sb.append(editable.subSequence(0, userNameStartPos))
            val spanStart = sb.length
            sb.append("@$userName")
            val spanEnd = sb.length
            if (userNameEndPos < editable.length) {
                sb.append(editable.subSequence(userNameEndPos, editable.length - 1))
            }

            createUserNameSpan(sb, spanStart, spanEnd)
            text = sb
            setSelection(spanEnd)
            onCancelUserNameEntry()
        } finally {
            isUserNameCommitting = false
        }
    }

    private fun createUserNameSpan(spannable: Spannable, start: Int, end: Int) {
        val span = UserColorSpan(ResourceUtil.getThemedColor(context, R.attr.colorAccent))
        spannable.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    /**
     * Returns the text of this field, with properly expanded user mentions.
     * Each user mention (in th form of @{username}) will be expanded into a wiki link to the
     * user page of that user, i.e. [[User:username|@username]]
     */
    fun getParsedText(wikiSite: WikiSite): String {
        var str = editable.toString()

        val spans = editable.getSpans(0, editable.length, UserColorSpan::class.java)
        if (spans.isNotEmpty()) {

            val pairs = mutableListOf<MutablePair<Int, Int>>()
            spans.forEach {
                pairs.add(MutablePair(editable.getSpanStart(it), editable.getSpanEnd(it)))
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

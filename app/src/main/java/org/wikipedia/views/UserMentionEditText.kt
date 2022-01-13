package org.wikipedia.views

import android.content.Context
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.core.widget.doOnTextChanged
import org.wikipedia.util.log.L

class UserMentionEditText : PlainPasteEditText {
    interface Listener {
        fun onStartUserNameEntry()
        fun onCancelUserNameEntry()
        fun onUserNameChanged(userName: String)
    }

    private val textWatcher: TextWatcher?
    var listener: Listener? = null

    private var userNameStartPos = -1
    private var userNameEndPos = -1
    private val isEnteringUserName get() = userNameStartPos >= 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        textWatcher = doOnTextChanged { text, start, before, count ->
            if (text == null) {
                return@doOnTextChanged
            }
            L.d(">>> text: $text, start: $start, before: $before, count: $count")
            if (start >= text.length) {
                return@doOnTextChanged
            }

            if (count == 1 && start < text.length && text[start] == '@' &&
                    (start == 0 || (start > 0 && text[start - 1] == ' ')) &&
                    !isEnteringUserName) {
                userNameStartPos = start
                onStartUserNameEntry()
            }

            if (isEnteringUserName) {
                if (count - before == 1 && start + count - 1 < text.length && start + count - 1 >= 0 &&
                        text[start + count - 1] == ' ') {
                    onCancelUserNameEntry()
                } else {
                    userNameEndPos += (count - before)
                }
            }
        }

    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (textWatcher == null) {
            return
        }
        L.d(">>> onSelectionChanged: $selStart, $selEnd")

        if (isEnteringUserName) {
            if (selStart < userNameStartPos || selEnd > userNameEndPos) {
                onCancelUserNameEntry()
                return
            }

            onUserNameChanged(text!!.substring(userNameStartPos, userNameEndPos))
        }
    }

    private fun onStartUserNameEntry() {
        userNameEndPos = userNameStartPos
        listener?.onStartUserNameEntry()
    }

    private fun onCancelUserNameEntry() {
        userNameStartPos = -1
        userNameEndPos = -1
        listener?.onCancelUserNameEntry()
    }

    private fun onUserNameChanged(userName: String) {
        L.d(">>> Current user name: $userName")
        listener?.onUserNameChanged(userName)
    }

    fun onCommitUserName(userName: String) {

    }
}

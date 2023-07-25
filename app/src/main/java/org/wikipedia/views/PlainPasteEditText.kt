package org.wikipedia.views

import android.content.ClipData
import android.content.Context
import android.os.Build
import android.text.InputType
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ContentInfoCompat
import androidx.core.view.ViewCompat
import com.google.android.material.textfield.TextInputEditText
import org.wikipedia.R

open class PlainPasteEditText : TextInputEditText {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        // The MIME type(s) need to be set for onReceiveContent() to be called.
        ViewCompat.setOnReceiveContentListener(rootView, arrayOf("text/*"), null)
        // applyCustomCursorDrawable()
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
        // For multiline EditTexts that specify a done keyboard action, unset the no carriage return
        // flag which otherwise limits the EditText to a single line
        val multilineInput = inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE == InputType.TYPE_TEXT_FLAG_MULTI_LINE
        val actionDone = outAttrs.imeOptions and EditorInfo.IME_ACTION_DONE == EditorInfo.IME_ACTION_DONE
        if (actionDone && multilineInput) {
            outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION.inv()
        }
        return super.onCreateInputConnection(outAttrs)
    }

    private fun applyCustomCursorDrawable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textCursorDrawable = AppCompatResources.getDrawable(context, R.drawable.custom_cursor)
        }
    }
}

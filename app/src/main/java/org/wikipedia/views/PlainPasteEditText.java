package org.wikipedia.views;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.design.widget.TextInputEditText;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import org.wikipedia.util.ClipboardUtil;

import static android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE;

public class PlainPasteEditText extends TextInputEditText {
    public PlainPasteEditText(Context context) {
        super(context);
    }

    public PlainPasteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlainPasteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        if (id == android.R.id.paste) {
            return onTextContextMenuPaste();
        }
        return super.onTextContextMenuItem(id);
    }

    @Override public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection connection = super.onCreateInputConnection(outAttrs);

        // For multiline EditTexts that specify a done keyboard action, unset the no carriage return
        // flag which otherwise limits the EditText to a single line
        boolean multilineInput = (getInputType() & TYPE_TEXT_FLAG_MULTI_LINE) == TYPE_TEXT_FLAG_MULTI_LINE;
        boolean actionDone = (outAttrs.imeOptions & EditorInfo.IME_ACTION_DONE) == EditorInfo.IME_ACTION_DONE;
        if (actionDone && multilineInput) {
            outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        }

        return connection;
    }

    private boolean onTextContextMenuPaste() {
        // Do not allow pasting of formatted text!
        // We do this by intercepting the clipboard and temporarily replacing its
        // contents with plain text.
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            ClipData oldClipData = clipboard.getPrimaryClip();
            String lastClipText = oldClipData.getItemAt(oldClipData.getItemCount() - 1).coerceToText(getContext()).toString();
            // temporarily set the new clip data as the primary
            ClipboardUtil.setPlainText(getContext(), null, lastClipText);
            // execute the paste!
            super.onTextContextMenuItem(android.R.id.paste);
            // restore the clip data back to the old one.
            clipboard.setPrimaryClip(oldClipData);
        }
        return true;
    }
}

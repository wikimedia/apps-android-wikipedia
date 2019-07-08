package org.wikipedia.views;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;

import org.wikipedia.edit.richtext.SpanExtents;
import org.wikipedia.edit.richtext.SyntaxHighlighter;
import org.wikipedia.util.ClipboardUtil;

import java.util.ArrayList;
import java.util.List;

import static android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE;

public class PlainPasteEditText extends TextInputEditText {
    public interface FindListener {
        void onFinished(int activeMatchOrdinal, int numberOfMatches, int textPosition, boolean findingNext);
    }

    @Nullable private InputConnection inputConnection;
    @Nullable private FindListener findListener;
    private List<Integer> findInPageTextPositionList = new ArrayList<>();
    private int findInPageCurrentTextPosition;
    private SyntaxHighlighter syntaxHighlighter;

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
        inputConnection = super.onCreateInputConnection(outAttrs);

        // For multiline EditTexts that specify a done keyboard action, unset the no carriage return
        // flag which otherwise limits the EditText to a single line
        boolean multilineInput = (getInputType() & TYPE_TEXT_FLAG_MULTI_LINE) == TYPE_TEXT_FLAG_MULTI_LINE;
        boolean actionDone = (outAttrs.imeOptions & EditorInfo.IME_ACTION_DONE) == EditorInfo.IME_ACTION_DONE;
        if (actionDone && multilineInput) {
            outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        }

        return inputConnection;
    }

    @Nullable
    public InputConnection getInputConnection() {
        return inputConnection;
    }

    public void undo() {
        if (inputConnection != null) {
            inputConnection.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON));
            inputConnection.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON));
        }
    }

    public void redo() {
        if (inputConnection != null) {
            inputConnection.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON));
            inputConnection.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON));
        }
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

    public void setFindListener(@NonNull FindListener listener) {
        this.findListener = listener;
    }

    public void findInEditor(@Nullable String targetText, @NonNull SyntaxHighlighter syntaxHighlighter) {
        if (findListener == null) {
            return;
        }
        this.syntaxHighlighter = syntaxHighlighter;
        findInPageCurrentTextPosition = 0;
        // apply find text syntax
        syntaxHighlighter.applyFindTextSyntax(targetText, new SyntaxHighlighter.OnSyntaxHighlightListener() {
            @Override
            public void syntaxHighlightResults(List<SpanExtents> spanExtents) {
                // ignore
            }

            @Override
            public void findTextMatches(List<SpanExtents> spanExtents) {
                findInPageTextPositionList.clear();
                for (SpanExtents spanExtent : spanExtents) {
                    findInPageTextPositionList.add(spanExtent.getStart());
                }
                onFinished(false);
            }
        });
    }

    public void findNext() {
        find(true);
    }

    public void findPrevious() {
        find(false);
    }

    private void find(boolean direction) {
        if (findListener == null) {
            return;
        }
        if (direction) {
            findInPageCurrentTextPosition = findInPageCurrentTextPosition == findInPageTextPositionList.size() - 1 ? 0 : ++findInPageCurrentTextPosition;
            onFinished(true);
        } else {
            findInPageCurrentTextPosition = findInPageCurrentTextPosition == 0 ? findInPageTextPositionList.size() - 1 : --findInPageCurrentTextPosition;
            onFinished(true);
        }
        syntaxHighlighter.setSelectedMatchResultPosition(findInPageCurrentTextPosition);
    }

    public void clearMatches(@NonNull SyntaxHighlighter syntaxHighlighter) {
        if (findListener == null) {
            return;
        }
        findInEditor(null, syntaxHighlighter);
    }

    private void onFinished(boolean findingNext) {
        if (findListener == null) {
            return;
        }
        findListener.onFinished(findInPageCurrentTextPosition,
                findInPageTextPositionList.size(),
                findInPageTextPositionList.isEmpty() ? 0 : findInPageTextPositionList.get(findInPageCurrentTextPosition),
                findingNext);
    }
}

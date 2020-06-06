package org.wikipedia.views;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.databinding.ViewWikitextKeyboardBinding;

public class WikiTextKeyboardView extends FrameLayout {
    public interface Callback {
        void onPreviewLink(@NonNull String title);
    }

    @Nullable private Callback callback;
    private PlainPasteEditText editText;

    public WikiTextKeyboardView(Context context) {
        super(context);
        init();
    }

    public WikiTextKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WikiTextKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        final ViewWikitextKeyboardBinding binding = ViewWikitextKeyboardBinding.bind(this);

        binding.wikitextButtonLink.setOnClickListener(v -> {
            if (editText.getInputConnection() != null) {
                toggleSyntaxAroundCurrentSelection(editText.getInputConnection(), "[[", "]]");
            }
        });
        binding.wikitextButtonItalic.setOnClickListener(v -> {
            if (editText.getInputConnection() != null) {
                toggleSyntaxAroundCurrentSelection(editText.getInputConnection(), "''", "''");
            }
        });
        binding.wikitextButtonBold.setOnClickListener(v -> {
            if (editText.getInputConnection() != null) {
                toggleSyntaxAroundCurrentSelection(editText.getInputConnection(), "'''", "'''");
            }
        });
        binding.wikitextButtonUndo.setOnClickListener(v -> editText.undo());
        binding.wikitextButtonRedo.setOnClickListener(v -> editText.redo());
        binding.wikitextButtonTemplate.setOnClickListener(v -> {
            if (editText.getInputConnection() != null) {
                toggleSyntaxAroundCurrentSelection(editText.getInputConnection(), "{{", "}}");
            }
        });
        binding.wikitextButtonRef.setOnClickListener(v -> {
            if (editText.getInputConnection() != null) {
                toggleSyntaxAroundCurrentSelection(editText.getInputConnection(), "<ref>", "</ref>");
            }
        });
        binding.wikitextButtonListBulleted.setOnClickListener(v -> {
            if (editText.getInputConnection() != null) {
                editText.getInputConnection().commitText("\n* ", 1);
            }
        });
        binding.wikitextButtonListNumbered.setOnClickListener(v -> {
            if (editText.getInputConnection() != null) {
                editText.getInputConnection().commitText("\n# ", 1);
            }
        });
        binding.wikitextButtonPreviewLink.setOnClickListener(this::onClickButtonPreviewLink);

        binding.wikitextButtonUndo.setVisibility(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP ? VISIBLE : GONE);
        binding.wikitextButtonRedo.setVisibility(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP ? VISIBLE : GONE);
        binding.wikitextUndoRedoSeparator.setVisibility(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP ? VISIBLE : GONE);
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void setEditText(@NonNull PlainPasteEditText editText) {
        this.editText = editText;
    }

    private void onClickButtonPreviewLink(View v) {
        if (editText.getInputConnection() == null) {
            return;
        }
        @Nullable String title = null;
        CharSequence selection = editText.getInputConnection().getSelectedText(0);
        if (selection != null && selection.length() > 0 && !selection.toString().contains("[[")) {
            title = trimPunctuation(selection.toString());
        } else {
            String before;
            String after;
            if (selection != null && selection.length() > 1) {
                String selectionStr = selection.toString();
                before = selectionStr.substring(0, selectionStr.length() / 2);
                after = selectionStr.substring(selectionStr.length() / 2);
            } else {
                final int peekLength = 64;
                before = editText.getInputConnection().getTextBeforeCursor(peekLength, 0).toString();
                after = editText.getInputConnection().getTextAfterCursor(peekLength, 0).toString();
            }
            if (TextUtils.isEmpty(before) || TextUtils.isEmpty(after)) {
                return;
            }
            String str = before + after;
            int i1 = lastIndexOf(before, "[[");
            int i2 = after.indexOf("]]") + before.length();
            if (i1 >= 0 && i2 > 0 && i2 > i1) {
                str = str.substring(i1 + 2, i2).trim();
                if (!TextUtils.isEmpty(str)) {
                    if (str.contains("|")) {
                        str = str.split("\\|")[0];
                    }
                    title = str;
                }
            }
        }
        if (title != null && callback != null) {
            callback.onPreviewLink(title);
        }
    }

    private void toggleSyntaxAroundCurrentSelection(InputConnection ic, @NonNull String prefix, @NonNull String suffix) {
        if (editText.getSelectionStart() == editText.getSelectionEnd()) {
            CharSequence before = ic.getTextBeforeCursor(prefix.length(), 0);
            CharSequence after = ic.getTextAfterCursor(suffix.length(), 0);
            if (before != null && before.toString().equals(prefix) && after != null && after.toString().equals(suffix)) {
                // the cursor is actually inside the exact syntax, so negate it.
                ic.deleteSurroundingText(prefix.length(), suffix.length());
            } else {
                // nothing highlighted, so just insert link syntax, and place the cursor in the center.
                ic.commitText(prefix + suffix, 1);
                ic.commitText("", -suffix.length());
            }
        } else {
            CharSequence selection = ic.getSelectedText(0);
            if (selection == null) {
                return;
            }
            if (selection.toString().startsWith(prefix) && selection.toString().endsWith(suffix)) {
                // the highlighted text is already a link, so toggle the link away.
                selection = selection.subSequence(prefix.length(), selection.length() - suffix.length());
            } else {
                // put link syntax around the highlighted text.
                selection = prefix + selection + suffix;
            }
            ic.commitText(selection, 1);
            ic.setSelection(editText.getSelectionStart() - selection.length(), editText.getSelectionEnd());
        }
    }

    private int lastIndexOf(@NonNull String str, @NonNull String subStr) {
        int index = -1;
        int a = 0;
        while (a < str.length()) {
            int i = str.indexOf(subStr, a);
            if (i >= 0) {
                index = i;
                a = i + 1;
            } else {
                break;
            }
        }
        return index;
    }

    private String trimPunctuation(@NonNull String str) {
        while (str.startsWith(".") || str.startsWith(",") || str.startsWith(";") || str.startsWith("?") || str.startsWith("!")) {
            str = str.substring(1);
        }
        while (str.endsWith(".") || str.endsWith(",") || str.endsWith(";") || str.endsWith("?") || str.endsWith("!")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }
}

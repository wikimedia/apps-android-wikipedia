package org.wikipedia.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;

import org.wikipedia.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class WikiTextKeyboardView extends FrameLayout {
    public interface Callback {
        void onPreviewLink(@NonNull String title);
        void onPreviewTemplate(@NonNull String title);
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
        inflate(getContext(), R.layout.view_wikitext_keyboard, this);
        ButterKnife.bind(this);
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void setEditText(@NonNull PlainPasteEditText editText) {
        this.editText = editText;
    }

    @OnClick(R.id.wikitext_button_link) void onClickButtonLink(View v) {
        if (editText.getInputConnection() != null) {
            toggleSyntaxAroundCurrentSelection(editText.getInputConnection(), "[[", "]]");
        }
    }

    @OnClick(R.id.wikitext_button_italic) void onClickButtonItalic(View v) {
        if (editText.getInputConnection() != null) {
            toggleSyntaxAroundCurrentSelection(editText.getInputConnection(), "''", "''");
        }
    }

    @OnClick(R.id.wikitext_button_bold) void onClickButtonBold(View v) {
        if (editText.getInputConnection() != null) {
            toggleSyntaxAroundCurrentSelection(editText.getInputConnection(), "'''", "'''");
        }
    }

    @OnClick(R.id.wikitext_button_template) void onClickButtonTemplate(View v) {
        if (editText.getInputConnection() != null) {
            toggleSyntaxAroundCurrentSelection(editText.getInputConnection(), "{{", "}}");
        }
    }

    @OnClick(R.id.wikitext_button_ref) void onClickButtonRef(View v) {
        if (editText.getInputConnection() != null) {
            toggleSyntaxAroundCurrentSelection(editText.getInputConnection(), "<ref>", "</ref>");
        }
    }

    @OnClick(R.id.wikitext_button_section) void onClickButtonSection(View v) {
        if (editText.getInputConnection() != null) {
            toggleSyntaxAroundCurrentSelection(editText.getInputConnection(), "==", "==");
        }
    }

    @OnClick(R.id.wikitext_button_subsection) void onClickButtonSubsection(View v) {
        if (editText.getInputConnection() != null) {
            toggleSyntaxAroundCurrentSelection(editText.getInputConnection(), "===", "===");
        }
    }

    @OnClick(R.id.wikitext_button_subsubsection) void onClickButtonSubsubsection(View v) {
        if (editText.getInputConnection() != null) {
            toggleSyntaxAroundCurrentSelection(editText.getInputConnection(), "====", "====");
        }
    }

    @OnClick(R.id.wikitext_button_endash) void onClickButtonEnDash(View v) {
        if (editText.getInputConnection() != null) {
            editText.getInputConnection().commitText("–", 1);
        }
    }

    @OnClick(R.id.wikitext_button_emdash) void onClickButtonEmDash(View v) {
        if (editText.getInputConnection() != null) {
            editText.getInputConnection().commitText("—", 1);
        }
    }

    @OnClick(R.id.wikitext_button_signature) void onClickButtonSignature(View v) {
        if (editText.getInputConnection() != null) {
            editText.getInputConnection().commitText("~~~~", 1);
        }
    }

    @OnClick(R.id.wikitext_button_list_bulleted) void onClickButtonListBulleted(View v) {
        if (editText.getInputConnection() != null) {
            editText.getInputConnection().commitText("\n* ", 1);
        }
    }

    @OnClick(R.id.wikitext_button_list_numbered) void onClickButtonListNumbered(View v) {
        if (editText.getInputConnection() != null) {
            editText.getInputConnection().commitText("\n# ", 1);
        }
    }

    @OnClick(R.id.wikitext_button_preview_link) void onClickButtonPreviewLink(View v) {
        if (editText.getInputConnection() == null) {
            return;
        }
        @Nullable String title = null;
        CharSequence selection = editText.getInputConnection().getSelectedText(0);
        if (selection != null && selection.length() > 0) {
            title = trimPunctuation(selection.toString()).replace("[[", "").replace("]]", "");
        } else {
            final int peekLength = 64;
            String before = editText.getInputConnection().getTextBeforeCursor(peekLength, 0).toString();
            String after = editText.getInputConnection().getTextAfterCursor(peekLength, 0).toString();
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

    @OnClick(R.id.wikitext_button_preview_template) void onClickButtonPreviewTemplate(View v) {
        if (editText.getInputConnection() == null) {
            return;
        }
        String text = editText.getText().toString();
        int cursorPos = editText.getSelectionStart();
        boolean foundBrace = false;
        int templateStartPos = -1;
        int templateEndPos = -1;

        for (int i = cursorPos; i >= 0; i--) {
            if (text.charAt(i) == '{') {
                if (foundBrace) {
                    templateStartPos = i + 2;
                    break;
                }
                foundBrace = true;
            }
        }
        foundBrace = false;
        for (int i = cursorPos; i < text.length(); i++) {
            if (text.charAt(i) == '}') {
                if (foundBrace) {
                    templateEndPos = i - 1;
                    break;
                }
                foundBrace = true;
            }
        }

        String templateName = "";

        if (templateStartPos >= 0 && templateEndPos > templateStartPos) {
            int pipePos = text.indexOf("|", templateStartPos);
            if (pipePos > templateStartPos && pipePos <= templateEndPos) {
                templateName = text.substring(templateStartPos, pipePos).trim();
            } else {
                templateName = text.substring(templateStartPos, templateEndPos);
            }
        }
        if (!TextUtils.isEmpty(templateName) && callback != null) {
            callback.onPreviewTemplate(templateName);
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

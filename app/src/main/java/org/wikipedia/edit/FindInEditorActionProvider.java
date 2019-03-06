package org.wikipedia.edit;

import android.graphics.Rect;
import androidx.annotation.NonNull;
import android.view.ActionMode;
import android.widget.ScrollView;

import org.wikipedia.edit.richtext.SyntaxHighlighter;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.views.FindInPageActionProvider;
import org.wikipedia.views.PlainPasteEditText;


public class FindInEditorActionProvider extends FindInPageActionProvider
        implements FindInPageActionProvider.FindInPageListener {
    @NonNull private final ActionMode actionMode;
    @NonNull private final PlainPasteEditText textView;
    @NonNull private final SyntaxHighlighter syntaxHighlighter;

    private ScrollView scrollView;

    public FindInEditorActionProvider(@NonNull ScrollView scrollView,
                                      @NonNull PlainPasteEditText textView,
                                      @NonNull SyntaxHighlighter syntaxHighlighter,
                                      @NonNull ActionMode actionMode) {
        super(textView.getContext());
        this.scrollView = scrollView;
        this.textView = textView;
        this.syntaxHighlighter = syntaxHighlighter;
        this.actionMode = actionMode;
        setListener(this);
    }

    public void findInPage(String s) {
        textView.setFindListener((activeMatchOrdinal, numberOfMatches, textPosition, findingNext) -> {
            setMatchesResults(activeMatchOrdinal, numberOfMatches);
            textView.setSelection(textPosition, textPosition + s.length());
            Rect r = new Rect();
            textView.getFocusedRect(r);
            final int scrollTopOffset = 32;
            scrollView.scrollTo(0, r.top - DimenUtil.roundedDpToPx(scrollTopOffset));
            if (findingNext) {
                textView.requestFocus();
            }
        });
        textView.findInEditor(s, syntaxHighlighter);
    }

    @Override
    public void onFindNextClicked() {
        textView.findNext();
    }

    @Override
    public void onFindNextLongClicked() {
        // TODO: implement this, the current applyFindTextSyntax will cause a crash.
    }

    @Override
    public void onFindPrevClicked() {
        textView.findPrevious();
    }

    @Override
    public void onFindPrevLongClicked() {
        // TODO: implement this, the current applyFindTextSyntax will cause a crash.
    }

    @Override
    public void onCloseClicked() {
        actionMode.finish();
    }

    @Override
    public void onSearchTextChanged(String text) {
        if (text.length() > 0) {
            findInPage(text);
        } else {
            textView.clearMatches(syntaxHighlighter);
            syntaxHighlighter.applyFindTextSyntax(text, null);
        }
    }
}

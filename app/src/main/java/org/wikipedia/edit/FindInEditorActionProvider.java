package org.wikipedia.edit;

import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.SearchView;
import android.view.ActionMode;
import android.view.ActionProvider;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.edit.richtext.SyntaxHighlighter;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.PlainPasteEditText;

public class FindInEditorActionProvider extends ActionProvider {
    @NonNull private final ActionMode actionMode;
    @NonNull private final PlainPasteEditText textView;
    @NonNull private final SyntaxHighlighter syntaxHighlighter;

    private ScrollView scrollView;
    private View findInPageNext;
    private View findInPagePrev;
    private TextView findInPageMatch;

    public FindInEditorActionProvider(@NonNull ScrollView scrollView, @NonNull PlainPasteEditText textView, @NonNull SyntaxHighlighter syntaxHighlighter, @NonNull ActionMode actionMode) {
        super(textView.getContext());
        this.scrollView = scrollView;
        this.textView = textView;
        this.syntaxHighlighter = syntaxHighlighter;
        this.actionMode = actionMode;
    }

    @Override
    public boolean overridesItemVisibility() {
        return true;
    }

    @Override
    public View onCreateActionView() {
        View view = View.inflate(textView.getContext(), R.layout.group_find_in_page, null);
        findInPageNext = view.findViewById(R.id.find_in_page_next);
        findInPageNext.setOnClickListener(v -> {
            DeviceUtil.hideSoftKeyboard(v);
            textView.findNext();
        });

        findInPagePrev = view.findViewById(R.id.find_in_page_prev);
        findInPagePrev.setOnClickListener(v -> {
            DeviceUtil.hideSoftKeyboard(v);
            textView.findPrevious();
        });

        // TODO: advanced searching features: long pressing on buttons will go to the last or first match of the result. (see findInPageActionProvider)

        setFindInPageChevronsEnabled(false);

        findInPageMatch = view.findViewById(R.id.find_in_page_match);
        View closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> actionMode.finish());

        SearchView searchView = view.findViewById(R.id.find_in_page_input);
        searchView.setQueryHint(searchView.getContext().getString(R.string.menu_page_find_in_page));
        searchView.setFocusable(true);
        searchView.requestFocusFromTouch();
        searchView.setOnQueryTextListener(searchQueryListener);
        searchView.setIconified(false);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        searchView.setSubmitButtonEnabled(false);
        // remove focus line from search plate
        View searchEditPlate = searchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
        searchEditPlate.setBackgroundColor(Color.TRANSPARENT);
        // remove the close icon in search view
        ImageView searchCloseButton = searchView.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
        searchCloseButton.setEnabled(false);
        searchCloseButton.setImageDrawable(null);

        return view;
    }

    private final SearchView.OnQueryTextListener searchQueryListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String s) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            if (s.length() > 0) {
                findInPage(s);
            } else {
                textView.clearMatches(syntaxHighlighter);
                syntaxHighlighter.applyFindTextSyntax(s, null);
                findInPageMatch.setVisibility(View.GONE);
                setFindInPageChevronsEnabled(false);
            }
            return true;
        }
    };

    public void findInPage(String s) {
        textView.setFindListener((activeMatchOrdinal, numberOfMatches, textPosition, findingNext) -> {
            if (numberOfMatches > 0) {
                findInPageMatch.setText(textView.getContext().getString(R.string.find_in_page_result,
                        activeMatchOrdinal + 1, numberOfMatches));
                findInPageMatch.setTextColor(ResourceUtil.getThemedColor(textView.getContext(), R.attr.material_theme_de_emphasised_color));
                setFindInPageChevronsEnabled(true);
                textView.setSelection(textPosition, textPosition + s.length());
                Rect r = new Rect();
                textView.getFocusedRect(r);
                final int scrollTopOffset = 32;
                scrollView.scrollTo(0, r.top - DimenUtil.roundedDpToPx(scrollTopOffset));
                if (findingNext) {
                    textView.requestFocus();
                }
            } else {
                findInPageMatch.setText("0/0");
                findInPageMatch.setTextColor(ResourceUtil.getThemedColor(textView.getContext(), R.attr.colorError));
                setFindInPageChevronsEnabled(false);
            }
            findInPageMatch.setVisibility(View.VISIBLE);
        });
        textView.findInEditor(s, syntaxHighlighter);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void setFindInPageChevronsEnabled(boolean enabled) {
        findInPageNext.setEnabled(enabled);
        findInPagePrev.setEnabled(enabled);
        findInPageNext.setAlpha(enabled ? 1.0f : 0.5f);
        findInPagePrev.setAlpha(enabled ? 1.0f : 0.5f);
    }
}

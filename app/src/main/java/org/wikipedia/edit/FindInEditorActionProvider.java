package org.wikipedia.edit;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SearchView;
import android.view.ActionProvider;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.ResourceUtil;

public class FindInEditorActionProvider extends ActionProvider {
    @NonNull private final EditSectionActivity activity;

    private View findInPageNext;
    private View findInPagePrev;
    private TextView findInPageMatch;
    private SearchView searchView;

    public FindInEditorActionProvider(@NonNull EditSectionActivity activity) {
        super(activity);
        this.activity = activity;
    }

    @Override
    public boolean overridesItemVisibility() {
        return true;
    }

    @Override
    public View onCreateActionView() {
        View view = View.inflate(activity, R.layout.group_find_in_page, null);
        findInPageNext = view.findViewById(R.id.find_in_page_next);
        findInPageNext.setOnClickListener(v -> {
            DeviceUtil.hideSoftKeyboard(v);
            if (activityEditorInvalid()) {
                return;
            }
            activity.getEditorView().findNext(true);
        });

        findInPagePrev = view.findViewById(R.id.find_in_page_prev);
        findInPagePrev.setOnClickListener(v -> {
            DeviceUtil.hideSoftKeyboard(v);
            if (activityEditorInvalid()) {
                return;
            }
            activity.getEditorView().findNext(false);
        });

        // TODO: advanced searching features: long pressing on buttons will go to the last or first match of the result. (see findInPageActionProvider)

        setFindInPageChevronsEnabled(false);

        findInPageMatch = view.findViewById(R.id.find_in_page_match);
        View closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> activity.closeFindInPage());

        searchView = view.findViewById(R.id.find_in_page_input);
        searchView.setQueryHint(activity.getString(R.string.menu_page_find_in_page));
        searchView.setFocusable(true);
        searchView.requestFocusFromTouch();
        searchView.setOnQueryTextListener(searchQueryListener);
        searchView.setIconified(false);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        searchView.setSubmitButtonEnabled(false);
        // remove focus line from search plate
        View searchEditPlate = searchView
                .findViewById(android.support.v7.appcompat.R.id.search_plate);
        searchEditPlate.setBackgroundColor(Color.TRANSPARENT);
        // remove the close icon in search view
        ImageView searchCloseButton = searchView
                .findViewById(android.support.v7.appcompat.R.id.search_close_btn);
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
            if (activityEditorInvalid()) {
                return false;
            }
            if (s.length() > 0) {
                findInPage(s);
            } else {
                activity.getEditorView().clearMatches(activity.getSyntaxHighlighter());
                activity.getSyntaxHighlighter().applyFindTextSyntax(s, null);
                findInPageMatch.setVisibility(View.GONE);
                setFindInPageChevronsEnabled(false);
            }
            return true;
        }
    };

    private boolean activityEditorInvalid() {
        return activity.getEditorView() == null;
    }

    public void findInPage(String s) {
        activity.getEditorView().setFindListener((activeMatchOrdinal, numberOfMatches, textPosition, findingNext) -> {
            if (numberOfMatches > 0) {
                findInPageMatch.setText(activity.getString(R.string.find_in_page_result,
                        activeMatchOrdinal + 1, numberOfMatches));
                findInPageMatch.setTextColor(ResourceUtil.getThemedColor(activity, R.attr.material_theme_de_emphasised_color));
                setFindInPageChevronsEnabled(true);

                if (findingNext) {
                    // TODO: scroll to the text position while searching
                    if (activity.getEditorView().getSelectionStart() != textPosition) {
                        activity.sectionScrollView.fullScroll(View.FOCUS_DOWN);
                    }
                    activity.getEditorView().post(() -> activity.getEditorView().setSelection(textPosition, textPosition + s.length()));
                } else {
                    searchView.requestFocus();
                }

            } else {
                findInPageMatch.setText("0/0");
                findInPageMatch.setTextColor(ContextCompat.getColor(activity, R.color.red50));
                setFindInPageChevronsEnabled(false);
            }
            findInPageMatch.setVisibility(View.VISIBLE);
        });
        activity.getEditorView().findInEditor(s, activity.getSyntaxHighlighter());
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void setFindInPageChevronsEnabled(boolean enabled) {
        findInPageNext.setEnabled(enabled);
        findInPagePrev.setEnabled(enabled);
        findInPageNext.setAlpha(enabled ? 1.0f : 0.5f);
        findInPagePrev.setAlpha(enabled ? 1.0f : 0.5f);
    }
}

package org.wikipedia.page;

import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.util.ApiUtil;

import android.graphics.Color;
import android.support.v4.view.ActionProvider;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView.FindListener;
import android.widget.TextView;

public class FindInPageActionProvider extends ActionProvider {

    private PageActivity parentActivity;

    private View findInPageNext;
    private View findInPagePrev;
    private TextView findInPageMatch;

    public FindInPageActionProvider(PageActivity parentActivity) {
        super(parentActivity);
        this.parentActivity = parentActivity;
    }

    @Override
    public boolean overridesItemVisibility() {
        return true;
    }

    @Override
    public View onCreateActionView() {
        View view = View.inflate(parentActivity, R.layout.group_find_in_page, null);
        findInPageNext = view.findViewById(R.id.find_in_page_next);
        findInPageNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.hideSoftKeyboard(parentActivity);
                if (!pageFragmentValid()) {
                    return;
                }
                parentActivity.getCurPageFragment().getWebView().findNext(true);
            }
        });

        findInPagePrev = view.findViewById(R.id.find_in_page_prev);
        findInPagePrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.hideSoftKeyboard(parentActivity);
                if (!pageFragmentValid()) {
                    return;
                }
                parentActivity.getCurPageFragment().getWebView().findNext(false);
            }
        });

        findInPageMatch = (TextView) view.findViewById(R.id.find_in_page_match);

        SearchView searchView = (SearchView) view.findViewById(R.id.find_in_page_input);
        searchView.setQueryHint(parentActivity.getString(R.string.menu_page_find_in_page));
        searchView.setFocusable(true);
        searchView.requestFocusFromTouch();
        searchView.setOnQueryTextListener(searchQueryListener);
        searchView.setOnCloseListener(searchCloseListener);
        searchView.setIconified(false);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        searchView.setSubmitButtonEnabled(false);
        // remove focus line from search plate
        View searchEditPlate = searchView
                .findViewById(android.support.v7.appcompat.R.id.search_plate);
        searchEditPlate.setBackgroundColor(Color.TRANSPARENT);
        return view;
    }

    private final SearchView.OnQueryTextListener searchQueryListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String s) {
            parentActivity.getCurPageFragment().closeFindInPage();
            return false;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            findInPageNext.setEnabled(s.length() > 0);
            findInPagePrev.setEnabled(s.length() > 0);
            if (!pageFragmentValid()) {
                return false;
            }
            if (s.length() > 0) {
                findInPage(s);
            } else {
                parentActivity.getCurPageFragment().getWebView().clearMatches();
                findInPageMatch.setVisibility(View.GONE);
            }
            return true;
        }
    };

    private final SearchView.OnCloseListener searchCloseListener = new SearchView.OnCloseListener() {
        @Override
        public boolean onClose() {
            parentActivity.getCurPageFragment().closeFindInPage();
            return false;
        }
    };

    private boolean pageFragmentValid() {
        if (parentActivity.getCurPageFragment() == null) {
            // could happen when we restore state
            return false;
        }
        if (parentActivity.getCurPageFragment().getWebView() == null) {
            // fragment instantiated, but not yet bound to activity
            return false;
        }
        return true;
    }

    public void findInPage(String s) {
        // to make it stop complaining
        if (ApiUtil.hasJellyBean()) {
            parentActivity.getCurPageFragment().getWebView().setFindListener(new FindListener() {
                @Override
                public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
                    if (!isDoneCounting) {
                        return;
                    }
                    if (numberOfMatches > 0) {
                        findInPageMatch.setText(
                                Integer.toString(activeMatchOrdinal + 1)
                                        + "/"
                                        + Integer.toString(numberOfMatches)
                        );
                        findInPageNext.setEnabled(true);
                        findInPagePrev.setEnabled(true);
                    } else {
                        findInPageMatch.setText("0/0");
                        findInPageNext.setEnabled(false);
                        findInPagePrev.setEnabled(false);
                    }
                    findInPageMatch.setVisibility(View.VISIBLE);
                }
            });
            parentActivity.getCurPageFragment().getWebView().findAllAsync(s);
        } else {
            parentActivity.getCurPageFragment().getWebView().findAll(s);
        }
    }
}

package org.wikipedia.page;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.SearchView;
import android.view.ActionProvider;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView.FindListener;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.analytics.FindInPageFunnel;
import org.wikipedia.util.DeviceUtil;

public class FindInPageActionProvider extends ActionProvider {
    @NonNull private final PageFragment fragment;
    @NonNull private final FindInPageFunnel funnel;

    private View findInPageNext;
    private View findInPagePrev;
    private TextView findInPageMatch;

    public FindInPageActionProvider(@NonNull PageFragment fragment,
                                    @NonNull FindInPageFunnel funnel) {
        super(fragment.getContext());
        this.fragment = fragment;
        this.funnel = funnel;
    }

    @Override
    public boolean overridesItemVisibility() {
        return true;
    }

    @Override
    public View onCreateActionView() {
        View view = View.inflate(fragment.getContext(), R.layout.group_find_in_page, null);
        findInPageNext = view.findViewById(R.id.find_in_page_next);
        findInPageNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeviceUtil.hideSoftKeyboard(view);
                if (!pageFragmentValid()) {
                    return;
                }
                funnel.addFindNext();
                fragment.getWebView().findNext(true);
            }
        });

        findInPagePrev = view.findViewById(R.id.find_in_page_prev);
        findInPagePrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fragment.hideSoftKeyboard();
                if (!pageFragmentValid()) {
                    return;
                }
                funnel.addFindPrev();
                fragment.getWebView().findNext(false);
            }
        });

        findInPageMatch = view.findViewById(R.id.find_in_page_match);

        SearchView searchView = view.findViewById(R.id.find_in_page_input);
        searchView.setQueryHint(fragment.getContext().getString(R.string.menu_page_find_in_page));
        searchView.setFocusable(true);
        searchView.requestFocusFromTouch();
        searchView.setOnQueryTextListener(searchQueryListener);
        searchView.setOnCloseListener(searchCloseListener);
        searchView.setIconified(false);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setInputType(EditorInfo.TYPE_CLASS_TEXT);
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
            return false;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            findInPageNext.setEnabled(s.length() > 0);
            findInPagePrev.setEnabled(s.length() > 0);
            if (!pageFragmentValid()) {
                return false;
            }
            funnel.setFindText(s);
            if (s.length() > 0) {
                findInPage(s);
            } else {
                fragment.getWebView().clearMatches();
                findInPageMatch.setVisibility(View.GONE);
            }
            return true;
        }
    };

    private final SearchView.OnCloseListener searchCloseListener = new SearchView.OnCloseListener() {
        @Override
        public boolean onClose() {
            fragment.closeFindInPage();
            return false;
        }
    };

    private boolean pageFragmentValid() {
        return fragment.getWebView() != null;
    }

    public void findInPage(String s) {
        fragment.getWebView().setFindListener(new FindListener() {
            @Override
            public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
                if (!isDoneCounting) {
                    return;
                }
                if (numberOfMatches > 0) {
                    findInPageMatch.setText(fragment.getString(R.string.find_in_page_result,
                            activeMatchOrdinal + 1, numberOfMatches));
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
        fragment.getWebView().findAllAsync(s);
    }
}

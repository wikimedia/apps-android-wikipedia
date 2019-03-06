package org.wikipedia.page;

import androidx.annotation.NonNull;

import org.wikipedia.analytics.FindInPageFunnel;
import org.wikipedia.views.FindInPageActionProvider;

public class FindInWebPageActionProvider extends FindInPageActionProvider
        implements FindInPageActionProvider.FindInPageListener {
    @NonNull private final PageFragment fragment;
    @NonNull private final FindInPageFunnel funnel;
    private String searchQuery;

    public FindInWebPageActionProvider(@NonNull PageFragment fragment,
                                       @NonNull FindInPageFunnel funnel) {
        super(fragment.requireContext());
        this.fragment = fragment;
        this.funnel = funnel;
        setListener(this);
    }

    public void findInPage(String s) {
        fragment.getWebView().setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
            if (!isDoneCounting) {
                return;
            }
            setMatchesResults(activeMatchOrdinal, numberOfMatches);
        });

        fragment.getWebView().findAllAsync(s);
    }

    @Override
    public void onFindNextClicked() {
        funnel.addFindNext();
        fragment.getWebView().findNext(true);
    }

    @Override
    public void onFindNextLongClicked() {
        // Go to the last match by going to the first one and then going one back.
        funnel.addFindPrev();
        fragment.getWebView().clearMatches();
        fragment.getWebView().findAllAsync(searchQuery);
    }

    @Override
    public void onFindPrevClicked() {
        funnel.addFindPrev();
        fragment.getWebView().findNext(false);
    }

    @Override
    public void onFindPrevLongClicked() {
        // Go to the first match by "restarting" the search.
        funnel.addFindNext();
        fragment.getWebView().clearMatches();
        fragment.getWebView().findAllAsync(searchQuery);
    }

    @Override
    public void onCloseClicked() {
        fragment.closeFindInPage();
    }

    @Override
    public void onSearchTextChanged(String text) {
        funnel.setFindText(text);
        if (text.length() > 0) {
            searchQuery = text;
            findInPage(text);
        } else {
            searchQuery = null;
            fragment.getWebView().clearMatches();
        }
    }
}

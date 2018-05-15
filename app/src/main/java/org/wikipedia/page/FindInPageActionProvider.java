package org.wikipedia.page;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SearchView;
import android.view.ActionProvider;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.wikipedia.R;
import org.wikipedia.analytics.FindInPageFunnel;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.ResourceUtil;

public class FindInPageActionProvider extends ActionProvider {
    @NonNull private final PageFragment fragment;
    @NonNull private final FindInPageFunnel funnel;

    private View findInPageNext;
    private View findInPagePrev;
    private TextView findInPageMatch;

    private String searchQuery;
    private boolean lastOccurrenceSearchFlag;
    private boolean isFirstOccurrence;
    private boolean isLastOccurrence;

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
        findInPageNext.setOnClickListener(v -> {
            DeviceUtil.hideSoftKeyboard(v);
            if (!pageFragmentValid()) {
                return;
            }
            funnel.addFindNext();
            fragment.getWebView().findNext(true);
        });
        findInPageNext.setOnLongClickListener(v -> {
            if (!pageFragmentValid() || searchQuery == null) {
                return true;
            }
            if (isLastOccurrence) {
                Toast.makeText(fragment.getContext(), fragment.getResources().getString(R.string.find_last_occurence), Toast.LENGTH_SHORT).show();
            } else {
                fragment.hideSoftKeyboard();
                // Go to the last match by going to the first one and then going one back.
                funnel.addFindPrev();
                fragment.getWebView().clearMatches();
                fragment.getWebView().findAllAsync(searchQuery);
                lastOccurrenceSearchFlag = true;
            }
            return true;
        });

        findInPagePrev = view.findViewById(R.id.find_in_page_prev);
        findInPagePrev.setOnClickListener(v -> {
            fragment.hideSoftKeyboard();
            if (!pageFragmentValid()) {
                return;
            }
            funnel.addFindPrev();
            fragment.getWebView().findNext(false);
        });
        findInPagePrev.setOnLongClickListener(v -> {
            if (!pageFragmentValid()) {
                return true;
            }
            if (isFirstOccurrence) {
                Toast.makeText(fragment.getContext(), fragment.getResources().getString(R.string.find_first_occurence), Toast.LENGTH_SHORT).show();
            } else {
                fragment.hideSoftKeyboard();
                // Go to the first match by "restarting" the search.
                funnel.addFindNext();
                fragment.getWebView().clearMatches();
                fragment.getWebView().findAllAsync(searchQuery);
            }
            return true;
        });

        setFindInPageChevronsEnabled(false);

        findInPageMatch = view.findViewById(R.id.find_in_page_match);
        View closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> fragment.closeFindInPage());

        SearchView searchView = view.findViewById(R.id.find_in_page_input);
        searchView.setQueryHint(fragment.getContext().getString(R.string.menu_page_find_in_page));
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
            if (!pageFragmentValid()) {
                return false;
            }
            funnel.setFindText(s);
            if (s.length() > 0) {
                searchQuery = s;
                findInPage(s);
            } else {
                searchQuery = null;
                fragment.getWebView().clearMatches();
                findInPageMatch.setVisibility(View.GONE);
                setFindInPageChevronsEnabled(false);
            }
            return true;
        }
    };

    private boolean pageFragmentValid() {
        return fragment.getWebView() != null;
    }

    public void findInPage(String s) {
        fragment.getWebView().setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
            if (!isDoneCounting) {
                return;
            }
            if (numberOfMatches > 0) {
                findInPageMatch.setText(fragment.getString(R.string.find_in_page_result,
                        activeMatchOrdinal + 1, numberOfMatches));
                findInPageMatch.setTextColor(ResourceUtil.getThemedColor(fragment.requireContext(), R.attr.material_theme_de_emphasised_color));
                setFindInPageChevronsEnabled(true);

                isFirstOccurrence = activeMatchOrdinal == 0;
                isLastOccurrence = activeMatchOrdinal + 1 == numberOfMatches;
            } else {
                findInPageMatch.setText("0/0");
                findInPageMatch.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.red50));
                setFindInPageChevronsEnabled(false);

                isFirstOccurrence = false;
                isLastOccurrence = false;
            }
            findInPageMatch.setVisibility(View.VISIBLE);

            if (lastOccurrenceSearchFlag) {
                // Go one occurrence back from the first one so it shows the last one.
                fragment.getWebView().findNext(false);
                lastOccurrenceSearchFlag = false;
            }
        });

        fragment.getWebView().findAllAsync(s);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void setFindInPageChevronsEnabled(boolean enabled) {
        findInPageNext.setEnabled(enabled);
        findInPagePrev.setEnabled(enabled);
        findInPageNext.setAlpha(enabled ? 1.0f : 0.5f);
        findInPagePrev.setAlpha(enabled ? 1.0f : 0.5f);
    }
}

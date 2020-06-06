package org.wikipedia.views;

import android.content.Context;
import android.graphics.Color;
import android.view.ActionProvider;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;

import org.wikipedia.R;
import org.wikipedia.databinding.GroupFindInPageBinding;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.ResourceUtil;

public class FindInPageActionProvider extends ActionProvider {
    public interface FindInPageListener {
        void onFindNextClicked();
        void onFindNextLongClicked();
        void onFindPrevClicked();
        void onFindPrevLongClicked();
        void onCloseClicked();
        void onSearchTextChanged(String text);
    }

    private View findInPageNext;
    private View findInPagePrev;
    private TextView findInPageMatch;
    private SearchView searchView;

    private Context context;
    private FindInPageListener listener;
    private boolean enableLastOccurrenceSearchFlag;
    private boolean lastOccurrenceSearchFlag;
    private boolean isFirstOccurrence;
    private boolean isLastOccurrence;

    public FindInPageActionProvider(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public boolean overridesItemVisibility() {
        return true;
    }

    @Override
    public View onCreateActionView() {
        View view = View.inflate(context, R.layout.group_find_in_page, null);
        final GroupFindInPageBinding binding = GroupFindInPageBinding.bind(view);

        findInPageNext = binding.findInPageNext;
        findInPageMatch = binding.findInPageMatch;
        findInPagePrev = binding.findInPagePrev;
        searchView = binding.findInPageInput;

        findInPageNext.setOnClickListener(v -> {
            DeviceUtil.hideSoftKeyboard(v);
            listener.onFindNextClicked();
        });
        findInPageNext.setOnLongClickListener(v -> {
            if (isLastOccurrence) {
                Toast.makeText(context, context.getString(R.string.find_last_occurence), Toast.LENGTH_SHORT).show();
            } else {
                DeviceUtil.hideSoftKeyboard(v);
                listener.onFindNextLongClicked();
                lastOccurrenceSearchFlag = true;
            }
            return true;
        });
        findInPagePrev.setOnClickListener(v -> {
            DeviceUtil.hideSoftKeyboard(v);
            listener.onFindPrevClicked();
        });
        findInPagePrev.setOnLongClickListener(v -> {
            if (isFirstOccurrence) {
                Toast.makeText(context, context.getString(R.string.find_first_occurence), Toast.LENGTH_SHORT).show();
            } else {
                DeviceUtil.hideSoftKeyboard(v);
                listener.onFindPrevLongClicked();
            }
            return true;
        });
        binding.closeButton.setOnClickListener(v -> listener.onCloseClicked());

        setFindInPageChevronsEnabled(false);
        searchView.setQueryHint(context.getString(R.string.menu_page_find_in_page));
        searchView.setFocusable(true);
        searchView.setOnQueryTextListener(searchQueryListener);
        searchView.setIconified(false);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        searchView.setSubmitButtonEnabled(false);
        // remove focus line from search plate
        View searchEditPlate = searchView.findViewById(androidx.appcompat.R.id.search_plate);
        searchEditPlate.setBackgroundColor(Color.TRANSPARENT);
        // remove the close icon in search view
        ImageView searchCloseButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        searchCloseButton.setEnabled(false);
        searchCloseButton.setImageDrawable(null);
        return view;
    }

    public void setListener(FindInPageListener listener) {
        this.listener = listener;
    }

    public void setSearchViewQuery(@NonNull String searchQuery) {
        searchView.setQuery(searchQuery, true);
    }

    public void setEnableLastOccurrenceSearchFlag(boolean enable) {
        enableLastOccurrenceSearchFlag = enable;
    }

    public void setMatchesResults(int activeMatchOrdinal, int numberOfMatches) {
        if (numberOfMatches > 0) {
            findInPageMatch.setText(context.getString(R.string.find_in_page_result,
                    activeMatchOrdinal + 1, numberOfMatches));
            findInPageMatch.setTextColor(ResourceUtil.getThemedColor(context, R.attr.material_theme_de_emphasised_color));
            setFindInPageChevronsEnabled(true);
            isFirstOccurrence = activeMatchOrdinal == 0;
            isLastOccurrence = activeMatchOrdinal + 1 == numberOfMatches;
        } else {
            findInPageMatch.setText("0/0");
            findInPageMatch.setTextColor(ResourceUtil.getThemedColor(context, R.attr.colorError));
            setFindInPageChevronsEnabled(false);
            isFirstOccurrence = false;
            isLastOccurrence = false;
        }
        if (enableLastOccurrenceSearchFlag && lastOccurrenceSearchFlag) {
            // Go one occurrence back from the first one so it shows the last one.
            lastOccurrenceSearchFlag = false;
            listener.onFindPrevClicked();
        }
        findInPageMatch.setVisibility(View.VISIBLE);
    }

    private final SearchView.OnQueryTextListener searchQueryListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String s) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            if (s.length() <= 0) {
                findInPageMatch.setVisibility(View.GONE);
                setFindInPageChevronsEnabled(false);
            }
            listener.onSearchTextChanged(s);
            return true;
        }
    };

    @SuppressWarnings("checkstyle:magicnumber")
    public void setFindInPageChevronsEnabled(boolean enabled) {
        findInPageNext.setEnabled(enabled);
        findInPagePrev.setEnabled(enabled);
        findInPageNext.setAlpha(enabled ? 1.0f : 0.5f);
        findInPagePrev.setAlpha(enabled ? 1.0f : 0.5f);
    }
}

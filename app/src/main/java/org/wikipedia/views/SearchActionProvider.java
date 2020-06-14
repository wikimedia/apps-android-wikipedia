package org.wikipedia.views;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.ActionProvider;

import org.wikipedia.R;
import org.wikipedia.databinding.GroupSearchBinding;
import org.wikipedia.util.DeviceUtil;

import static org.wikipedia.util.ResourceUtil.getThemedColor;

public class SearchActionProvider extends ActionProvider {

    public interface Callback {
        void onQueryTextChange(String s);
        void onQueryTextFocusChange();
    }

    private CabSearchView searchView;

    private Context context;
    private String searchHintString;
    private Callback callback;

    public SearchActionProvider(@NonNull Context context, @NonNull String searchHintString, @NonNull Callback callback) {
        super(context);
        this.context = context;
        this.searchHintString = searchHintString;
        this.callback = callback;
    }

    @Override
    public boolean overridesItemVisibility() {
        return true;
    }

    @Override
    public View onCreateActionView() {
        final GroupSearchBinding binding = GroupSearchBinding.bind(View.inflate(context, R.layout.group_search, null));
        searchView = binding.searchInput;
        searchView.setFocusable(true);
        searchView.setIconified(false);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        searchView.setSubmitButtonEnabled(false);
        searchView.setQueryHint(searchHintString);
        searchView.setSearchHintTextColor(getThemedColor(getContext(), R.attr.material_theme_de_emphasised_color));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                searchView.setCloseButtonVisibility(s);
                callback.onQueryTextChange(s);
                return true;
            }
        });

        searchView.setOnQueryTextFocusChangeListener((v, isFocus) -> {
            if (!isFocus) {
                callback.onQueryTextFocusChange();
            }
        });

        // remove focus line from search plate
        View searchEditPlate = searchView.findViewById(androidx.appcompat.R.id.search_plate);
        searchEditPlate.setBackgroundColor(Color.TRANSPARENT);

        DeviceUtil.showSoftKeyboard(searchView);
        return binding.getRoot();
    }
}

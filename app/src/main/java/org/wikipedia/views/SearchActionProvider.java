package org.wikipedia.views;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.ActionProvider;

import org.wikipedia.R;
import org.wikipedia.util.DeviceUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.wikipedia.util.ResourceUtil.getThemedColor;

public class SearchActionProvider extends ActionProvider {

    public interface Callback {
        void onQueryTextChange(String s);
        void onQueryTextFocusChange();
    }

    @BindView(R.id.search_input) CabSearchView searchView;

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
        View view = View.inflate(context, R.layout.group_search, null);
        ButterKnife.bind(this, view);
        searchView.setFocusable(true);
        searchView.requestFocusFromTouch();
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
        return view;
    }
}

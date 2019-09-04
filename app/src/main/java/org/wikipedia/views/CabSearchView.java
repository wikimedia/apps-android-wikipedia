package org.wikipedia.views;

import android.content.Context;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;

import org.wikipedia.R;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.util.FeedbackUtil;

import java.util.Arrays;

import static org.wikipedia.util.ResourceUtil.getThemedColor;

/** {@link SearchView} that exposes contextual action bar callbacks. */
public class CabSearchView extends SearchView {
    private ImageView searchCloseBtn;
    private SearchView.SearchAutoComplete searchSrcTextView;

    private static final int SEARCH_TEXT_SIZE = 16;

    public CabSearchView(Context context) {
        this(context, null);
    }

    public CabSearchView(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.appcompat.R.attr.searchViewStyle);
    }

    public CabSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        int themedIconColor = getThemedColor(getContext(), R.attr.page_toolbar_icon_color);
        searchSrcTextView = findViewById(R.id.search_src_text);
        searchSrcTextView.setTextColor(getThemedColor(getContext(), R.attr.primary_text_color));
        searchSrcTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, SEARCH_TEXT_SIZE);
        searchSrcTextView.setHintTextColor(themedIconColor);
        ImageView searchMagIcon = findViewById(R.id.search_mag_icon);
        searchMagIcon.setColorFilter(themedIconColor);
        searchCloseBtn = findViewById(R.id.search_close_btn);
        searchCloseBtn.setVisibility(GONE);
        searchCloseBtn.setColorFilter(themedIconColor);
        FeedbackUtil.setToolbarButtonLongPressToast(searchCloseBtn);
        addFilter(searchSrcTextView, new PlainTextInputFilter());
    }

    private void addFilter(TextView textView, InputFilter filter) {
        InputFilter[] filters = textView.getFilters();
        InputFilter[] newFilters = Arrays.copyOf(filters, filters.length + 1);
        newFilters[filters.length] = filter;
        textView.setFilters(newFilters);
    }

    public void selectAllQueryTexts() {
        searchSrcTextView.selectAll();
    }

    public void setSearchHintTextColor(int color) {
        searchSrcTextView.setHintTextColor(color);
    }

    public void setCloseButtonVisibility(String searchString) {
        if (TextUtils.isEmpty(searchString)) {
            searchCloseBtn.setVisibility(GONE);
            searchCloseBtn.setImageDrawable(null);

        } else {
            searchCloseBtn.setVisibility(VISIBLE);
            searchCloseBtn.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_close_themed_24dp));
        }
    }

    private static class PlainTextInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                                   int dstart, int dend) {
            return RichTextUtil.stripRichText(source, start, end).subSequence(start, end);
        }
    }
}

package org.wikipedia.views;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;

public class GoneIfEmptyTextView extends AppTextView {
    public GoneIfEmptyTextView(Context context) {
        super(context);
        init();
    }

    public GoneIfEmptyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GoneIfEmptyTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        addTextChangedListener(new EmptyTextWatcher());
    }

    private class EmptyTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            setVisibility(editable.length() == 0 ? View.GONE : View.VISIBLE);
        }
    }
}

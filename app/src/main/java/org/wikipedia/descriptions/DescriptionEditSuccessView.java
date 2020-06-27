package org.wikipedia.descriptions;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.databinding.ViewDescriptionEditSuccessBinding;
import org.wikipedia.views.AppTextViewWithImages;

public class DescriptionEditSuccessView extends FrameLayout {
    private AppTextViewWithImages hintTextView;

    @Nullable private Callback callback;

    public interface Callback {
        void onDismissClick();
    }

    public DescriptionEditSuccessView(Context context) {
        super(context);
        init();
    }

    public DescriptionEditSuccessView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DescriptionEditSuccessView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    private void init() {
        final ViewDescriptionEditSuccessBinding binding = ViewDescriptionEditSuccessBinding.bind(this);

        hintTextView = binding.viewDescriptionEditSuccessHintText;
        binding.viewDescriptionEditSuccessDoneButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.onDismissClick();
            }
        });

        setHintText();
    }

    private void setHintText() {
        String editHint = getResources().getString(R.string.description_edit_success_article_edit_hint);
        hintTextView.setTextWithDrawables(editHint, R.drawable.ic_mode_edit_white_24dp);
    }
}

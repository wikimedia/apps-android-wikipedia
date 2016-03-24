package org.wikipedia.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.wikipedia.R;

public class PasswordTextInput extends FrameLayout {
    private EditText editText;
    private View passwordShowButton;

    @Nullable
    private OnShowPasswordListener onShowPasswordListener;

    public interface OnShowPasswordListener {
        void onShowPasswordChecked(boolean checked);
    }

    public PasswordTextInput(Context context) {
        this(context, null);
    }

    public PasswordTextInput(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PasswordTextInput(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PasswordTextInput(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setOnShowPasswordListener(@Nullable OnShowPasswordListener listener) {
        onShowPasswordListener = listener;
    }

    public EditText getEditText() {
        return editText;
    }

    private void init() {
        inflate(getContext(), R.layout.view_password_text_input, this);
        editText = (EditText) findViewById(R.id.password_edit_text_input);
        passwordShowButton = findViewById(R.id.password_edit_text_show);
        passwordShowButton.setOnClickListener(new OnShowPasswordClickListener());
        updateButtonOpacity();
    }

    private boolean isPasswordVisible() {
        return (editText.getInputType() & InputType.TYPE_MASK_VARIATION)
                == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
    }

    private void setInputTypePassword(EditText editText) {
        editText.setInputType((editText.getInputType() & ~InputType.TYPE_MASK_VARIATION)
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }

    private void setInputTypeVisiblePassword(EditText editText) {
        editText.setInputType((editText.getInputType() & ~InputType.TYPE_MASK_VARIATION)
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
    }

    private void updateButtonOpacity() {
        final float hiddenOpacity = 0.4f;
        passwordShowButton.setAlpha(isPasswordVisible() ? 1.0f : hiddenOpacity);
    }

    private class OnShowPasswordClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            boolean passwordVisible = isPasswordVisible();
            passwordVisible = !passwordVisible;
            int curPos = editText.getSelectionStart();
            if (passwordVisible) {
                setInputTypeVisiblePassword(editText);
            } else {
                setInputTypePassword(editText);
            }
            editText.setSelection(curPos);
            updateButtonOpacity();
            if (onShowPasswordListener != null) {
                onShowPasswordListener.onShowPasswordChecked(passwordVisible);
            }
        }
    }
}

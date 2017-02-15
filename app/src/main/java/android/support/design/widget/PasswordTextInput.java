package android.support.design.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;

import org.wikipedia.R;

public class PasswordTextInput extends TextInputLayout {
    public interface OnShowPasswordClickListener {
        void onShowPasswordClick(boolean visible);
    }

    @Nullable private OnShowPasswordClickListener onShowPasswordClickListener;

    public PasswordTextInput(Context context) {
        super(context);
        init();
    }

    public PasswordTextInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PasswordTextInput(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setOnShowPasswordListener(@Nullable OnShowPasswordClickListener listener) {
        onShowPasswordClickListener = listener;
    }

    public boolean isPasswordVisible() {
        return getEditText() != null
                && !(getEditText().getTransformationMethod() instanceof PasswordTransformationMethod);
    }

    @Override void passwordVisibilityToggleRequested() {
        super.passwordVisibilityToggleRequested();
        if (onShowPasswordClickListener != null) {
            onShowPasswordClickListener.onShowPasswordClick(isPasswordVisible());
        }
    }

    private void init() {
        inflate(getContext(), R.layout.view_password_text_input, this);
    }
}

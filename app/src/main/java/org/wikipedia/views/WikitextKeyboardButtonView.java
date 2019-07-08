package org.wikipedia.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import org.wikipedia.R;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class WikitextKeyboardButtonView extends FrameLayout {
    @BindView(R.id.wikitext_button_text) TextView buttonTextView;
    @BindView(R.id.wikitext_button_hint) TextView buttonHintView;
    @BindView(R.id.wikitext_button_image) ImageView buttonImageView;

    public WikitextKeyboardButtonView(Context context) {
        super(context);
        init(null, 0);
    }

    public WikitextKeyboardButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public WikitextKeyboardButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(@Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        inflate(getContext(), R.layout.view_wikitext_keyboard_button, this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ButterKnife.bind(this);
        if (attrs != null) {
            TypedArray array = getContext().obtainStyledAttributes(attrs,
                    R.styleable.WikitextKeyboardButtonView, defStyleAttr, 0);

            int drawableId = array.getResourceId(R.styleable.WikitextKeyboardButtonView_buttonImage, 0);
            String buttonText = array.getString(R.styleable.WikitextKeyboardButtonView_buttonText);
            String buttonHint = array.getString(R.styleable.WikitextKeyboardButtonView_buttonHint);
            int buttonTextColor = array.getColor(R.styleable.WikitextKeyboardButtonView_buttonTextColor,
                    ResourceUtil.getThemedColor(getContext(), R.attr.material_theme_secondary_color));

            if (drawableId != 0) {
                buttonTextView.setVisibility(GONE);
                buttonHintView.setVisibility(GONE);
                buttonImageView.setVisibility(VISIBLE);
                buttonImageView.setImageResource(drawableId);
            } else {
                buttonTextView.setVisibility(VISIBLE);
                buttonHintView.setVisibility(VISIBLE);
                buttonImageView.setVisibility(GONE);
                if (!TextUtils.isEmpty(buttonText)) {
                    buttonTextView.setText(buttonText);
                }
                buttonTextView.setTextColor(buttonTextColor);
                if (!TextUtils.isEmpty(buttonHint)) {
                    buttonHintView.setText(buttonHint);
                }
            }

            if (!TextUtils.isEmpty(buttonHint)) {
                setContentDescription(buttonHint);
                FeedbackUtil.setToolbarButtonLongPressToast(this);
            }
            array.recycle();
        }

        setClickable(true);
        setFocusable(true);
        setBackground(AppCompatResources.getDrawable(getContext(),
                ResourceUtil.getThemedAttributeId(getContext(), android.R.attr.selectableItemBackground)));
    }
}

package org.wikipedia.descriptions;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DescriptionEditTutorialPageView extends LinearLayout {
    interface Callback {
        void onButtonClick(@NonNull DescriptionEditTutorialPageView view);
    }

    @BindView(R.id.view_description_edit_tutorial_page_image) ImageView imageView;
    @BindView(R.id.view_description_edit_tutorial_page_primary_text) TextView primaryTextView;
    @BindView(R.id.view_description_edit_tutorial_page_secondary_text) TextView secondaryTextView;
    @BindView(R.id.view_description_edit_tutorial_page_tertiary_text) TextView tertiaryTextView;
    @BindView(R.id.view_description_edit_tutorial_page_button) TextView button;

    @Nullable private Callback callback;

    public DescriptionEditTutorialPageView(Context context) {
        super(context);
        init(null, 0, 0);
    }

    public DescriptionEditTutorialPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0, 0);
    }

    public DescriptionEditTutorialPageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DescriptionEditTutorialPageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    @OnClick(R.id.view_description_edit_tutorial_page_button) void onButtonClick() {
        if (callback != null) {
            callback.onButtonClick(this);
        }
    }

    private void init(@Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        inflate(getContext(), R.layout.view_description_edit_tutorial_page, this);
        ButterKnife.bind(this);
        if (attrs != null) {
            TypedArray array = getContext().obtainStyledAttributes(attrs,
                    R.styleable.DescriptionEditTutorialPageView, defStyleAttr, defStyleRes);
            Drawable image = array.getDrawable(R.styleable.DescriptionEditTutorialPageView_image);
            String primaryText = array.getString(R.styleable.DescriptionEditTutorialPageView_primaryText);
            String secondaryText = array.getString(R.styleable.DescriptionEditTutorialPageView_secondaryText);
            String tertiaryText = array.getString(R.styleable.DescriptionEditTutorialPageView_tertiaryText);
            String buttonText = array.getString(R.styleable.DescriptionEditTutorialPageView_buttonText);

            imageView.setImageDrawable(image);
            primaryTextView.setText(primaryText);
            secondaryTextView.setText(secondaryText);
            tertiaryTextView.setText(tertiaryText);
            button.setText(buttonText);

            array.recycle();
        }
    }
}

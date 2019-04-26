package org.wikipedia.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;

@SuppressLint("ViewConstructor")
public class DialogTitleWithImage extends LinearLayout {
    @BindView(R.id.title) TextView titleView;
    @BindView(R.id.image) AppCompatImageView image;
    private boolean preserveImageAspect;

    public DialogTitleWithImage(@NonNull Context context, @StringRes int titleRes, @DrawableRes int imageRes, boolean preserveImageAspect) {
        super(context);
        this.preserveImageAspect = preserveImageAspect;
        setOrientation(VERTICAL);
        inflate(context, R.layout.view_dialog_title_with_image, this);
        ButterKnife.bind(this);
        titleView.setText(titleRes);
        image.setImageResource(imageRes);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (image.getDrawable() != null && preserveImageAspect) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)image.getLayoutParams();
            params.height = (int) (((double)image.getDrawable().getIntrinsicHeight() / image.getDrawable().getIntrinsicWidth()) * image.getWidth());
            image.setLayoutParams(params);
        }
    }
}

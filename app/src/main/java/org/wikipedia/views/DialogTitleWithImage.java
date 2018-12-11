package org.wikipedia.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.AppCompatImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;

@SuppressLint("ViewConstructor")
public class DialogTitleWithImage extends LinearLayout {
    @BindView(R.id.title) TextView titleView;
    @BindView(R.id.image) AppCompatImageView image;

    public DialogTitleWithImage(@NonNull Context context, @StringRes int titleRes, @DrawableRes int imageRes) {
        super(context);
        setOrientation(VERTICAL);
        inflate(context, R.layout.view_dialog_title_with_image, this);
        ButterKnife.bind(this);
        titleView.setText(titleRes);
        image.setImageResource(imageRes);
    }
}

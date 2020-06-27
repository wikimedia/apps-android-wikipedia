package org.wikipedia.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;

import org.wikipedia.databinding.ViewDialogTitleWithImageBinding;

@SuppressLint("ViewConstructor")
public class DialogTitleWithImage extends LinearLayout {
    private AppCompatImageView image;
    private boolean preserveImageAspect;

    public DialogTitleWithImage(@NonNull Context context, @StringRes int titleRes, @DrawableRes int imageRes, boolean preserveImageAspect) {
        super(context);
        this.preserveImageAspect = preserveImageAspect;
        setOrientation(VERTICAL);

        final ViewDialogTitleWithImageBinding binding = ViewDialogTitleWithImageBinding.bind(this);
        image = binding.image;

        binding.title.setText(titleRes);
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

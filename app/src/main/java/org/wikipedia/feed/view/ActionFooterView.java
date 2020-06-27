package org.wikipedia.feed.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import org.wikipedia.databinding.ViewCardActionFooterBinding;

public class ActionFooterView extends ConstraintLayout {
    private View actionButton;
    private ImageView actionIcon;
    private TextView actionText;
    private View shareButton;

    public ActionFooterView(Context context) {
        super(context);
        init();
    }

    public ActionFooterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ActionFooterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        final ViewCardActionFooterBinding binding = ViewCardActionFooterBinding.bind(this);

        actionButton = binding.viewCardActionFooterButton;
        actionIcon = binding.viewCardActionFooterButtonIcon;
        actionText = binding.viewCardActionFooterButtonText;
        shareButton = binding.viewCardActionFooterShareButton;
    }

    public ActionFooterView actionIcon(@DrawableRes int resId) {
        actionIcon.setImageResource(resId);
        return this;
    }

    public void actionIconColor(@ColorRes int color) {
        actionIcon.setColorFilter(ContextCompat.getColor(getContext(), color));
    }

    public ActionFooterView actionText(@StringRes int resId) {
        actionText.setText(getResources().getString(resId));
        return this;
    }

    public void actionTextColor(@ColorRes int color) {
        actionText.setTextColor(ContextCompat.getColor(getContext(), color));
    }

    public ActionFooterView onActionListener(@Nullable OnClickListener listener) {
        actionButton.setOnClickListener(listener);
        return this;
    }

    public ActionFooterView onShareListener(@Nullable OnClickListener listener) {
        shareButton.setOnClickListener(listener);
        return this;
    }
}

package org.wikipedia.feed.progress;

import android.content.Context;
import android.widget.FrameLayout;

import org.wikipedia.R;

public class ProgressCardView extends FrameLayout {
    public ProgressCardView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_progress, this);
    }
}
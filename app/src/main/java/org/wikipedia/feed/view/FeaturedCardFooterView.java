package org.wikipedia.feed.view;

import android.content.Context;
import android.widget.RelativeLayout;

import org.wikipedia.R;

public class FeaturedCardFooterView extends RelativeLayout {
    public FeaturedCardFooterView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_featured_footer, this);
    }
}

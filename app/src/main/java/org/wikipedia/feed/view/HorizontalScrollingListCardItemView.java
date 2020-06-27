package org.wikipedia.feed.view;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import org.wikipedia.R;
import org.wikipedia.databinding.ViewHorizontalScrollListItemCardBinding;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;

public class HorizontalScrollingListCardItemView extends CardView {
    private FaceAndColorDetectImageView imageView;
    private TextView textView;
    @Nullable private FeedAdapter.Callback callback;

    public HorizontalScrollingListCardItemView(@NonNull Context context) {
        super(context);

        final ViewHorizontalScrollListItemCardBinding binding = ViewHorizontalScrollListItemCardBinding.bind(this);

        imageView = binding.horizontalScrollListItemImage;
        textView = binding.horizontalScrollListItemText;

        setCardBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color));
        setFocusable(true);
    }

    public void setCallback(@Nullable FeedAdapter.Callback callback) {
        this.callback = callback;
    }

    @Nullable
    public FeedAdapter.Callback getCallback() {
        return callback;
    }

    public void setText(@NonNull CharSequence text) {
        textView.setText(text);
        RichTextUtil.removeUnderlinesFromLinksAndMakeBold(textView);
    }

    public void setImage(@Nullable Uri image) {
        if (image == null) {
            imageView.setVisibility(GONE);
            textView.setMaxLines(10);
        } else {
            imageView.setVisibility(VISIBLE);
            imageView.loadImage(image);
        }
    }

    public View getImageView() {
        return imageView;
    }
}

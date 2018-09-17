package org.wikipedia.main.floatingqueue;

import android.content.Context;
import android.graphics.PointF;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import org.wikipedia.R;
import org.wikipedia.views.FaceAndColorDetectImageView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FloatingQueueImageView extends FrameLayout {

    // TODO: convert this file to Kotlin

    @BindView(R.id.view_floating_queue_image)
    FaceAndColorDetectImageView image;

    public FloatingQueueImageView(Context context) {
        super(context);
        init();
    }

    public FloatingQueueImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FloatingQueueImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void load(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            image.loadImage(R.drawable.ic_image_gray_opaque_24dp);
        } else {
            image.loadImage(Uri.parse(url));
        }
    }

    public FaceAndColorDetectImageView getImage() {
        return image;
    }

    private void init() {
        inflate(getContext(), R.layout.view_floating_queue_image, this);
        ButterKnife.bind(this);

        image.setOnImageLoadListener(new FaceAndColorDetectImageView.OnImageLoadListener() {
            @Override
            public void onImageLoaded(int bmpHeight, @Nullable PointF faceLocation, int mainColor) {
                if (faceLocation != null) {
                    image.getHierarchy().setActualImageFocusPoint(faceLocation);
                }
            }

            @Override
            public void onImageFailed() {
            }
        });
    }
}

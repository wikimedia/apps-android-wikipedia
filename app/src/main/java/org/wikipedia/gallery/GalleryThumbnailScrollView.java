package org.wikipedia.gallery;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.wikipedia.R;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.ViewUtil;

import java.util.List;

public class GalleryThumbnailScrollView extends RecyclerView {
    @NonNull private final Animation mPressAnimation;
    @NonNull private final Animation mReleaseAnimation;
    @Nullable private GalleryViewListener mListener;

    public GalleryThumbnailScrollView(Context context) {
        this(context, null);
    }

    public GalleryThumbnailScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GalleryThumbnailScrollView(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

        mPressAnimation = AnimationUtils.loadAnimation(context, R.anim.thumbnail_item_press);
        mReleaseAnimation = AnimationUtils.loadAnimation(context, R.anim.thumbnail_item_release);
    }

    public interface GalleryViewListener {
        void onGalleryItemClicked(@NonNull ImageView view, @NonNull String thumbUrl, @NonNull String imageName);
    }

    public void setGalleryViewListener(@Nullable GalleryViewListener listener) {
        mListener = listener;
    }

    public void setGalleryList(@NonNull List<MwQueryPage> list) {
        setAdapter(new GalleryViewAdapter(list));
    }

    private class GalleryItemHolder extends ViewHolder implements OnClickListener, OnTouchListener {
        private final ImageView mImageView;
        private MwQueryPage mGalleryItem;

        GalleryItemHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.gallery_thumbnail_image);
        }

        public void bindItem(MwQueryPage item) {
            mGalleryItem = item;
            mImageView.setFocusable(true);
            mImageView.setOnClickListener(this);
            mImageView.setOnTouchListener(this);
            ViewUtil.loadImage(mImageView, mGalleryItem.imageInfo().getThumbUrl());
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onGalleryItemClicked((ImageView)v, mGalleryItem.imageInfo().getThumbUrl(), StringUtil.addUnderscores(mGalleryItem.title()));
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.startAnimation(mPressAnimation);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.startAnimation(mReleaseAnimation);
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    private final class GalleryViewAdapter extends RecyclerView.Adapter<GalleryItemHolder> {
        @NonNull private final List<MwQueryPage> list;

        GalleryViewAdapter(@NonNull List<MwQueryPage> list) {
            this.list = list;
        }

        @Override
        public int getItemCount() {
            return  list.size();
        }

        @Override
        public GalleryItemHolder onCreateViewHolder(ViewGroup parent, int pos) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gallery_thumbnail, parent, false);
            return new GalleryItemHolder(view);
        }

        @Override
        public void onBindViewHolder(GalleryItemHolder holder, int pos) {
            holder.bindItem(list.get(pos));
        }
    }
}

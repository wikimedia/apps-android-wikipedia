package org.wikipedia.gallery;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.R;
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
        void onGalleryItemClicked(String imageName);
    }

    public void setGalleryViewListener(@Nullable GalleryViewListener listener) {
        mListener = listener;
    }

    public void setGalleryList(@NonNull List<GalleryItem> list) {
        setAdapter(new GalleryViewAdapter(list));
    }

    private class GalleryItemHolder extends ViewHolder implements OnClickListener, OnTouchListener {
        private final SimpleDraweeView mImageView;
        private GalleryItem mGalleryItem;

        GalleryItemHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.gallery_thumbnail_image);
        }

        public void bindItem(GalleryItem item) {
            mGalleryItem = item;
            mImageView.setOnClickListener(this);
            mImageView.setOnTouchListener(this);
            ViewUtil.loadImageUrlInto(mImageView, mGalleryItem.getThumbnailUrl());
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                // TODO: check which title will be used here
                mListener.onGalleryItemClicked(mGalleryItem.getTitles().getCanonical());
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
        @NonNull private final List<GalleryItem> list;

        GalleryViewAdapter(@NonNull List<GalleryItem> list) {
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

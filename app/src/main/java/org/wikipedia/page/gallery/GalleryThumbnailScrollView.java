package org.wikipedia.page.gallery;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class GalleryThumbnailScrollView extends RecyclerView {
    @NonNull private final Context mContext;
    @Nullable private GalleryViewListener mListener;

    private final OnClickListener mItemClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListener != null) {
                GalleryItem item = (GalleryItem) v.getTag();
                mListener.onGalleryItemClicked(item.getName());
            }
        }
    };

    public GalleryThumbnailScrollView(Context context) {
        this(context, null);
    }

    public GalleryThumbnailScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GalleryThumbnailScrollView(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
    }

    public interface GalleryViewListener {
        void onGalleryItemClicked(String imageName);
    }

    public void setGalleryViewListener(@Nullable GalleryViewListener listener) {
        mListener = listener;
    }

    public void setGalleryCollection(@NonNull GalleryCollection collection) {
        setAdapter(new GalleryViewAdapter(collection));
    }

    private class GalleryItemHolder extends ViewHolder {
        private final ImageView mImageView;
        private GalleryItem mGalleryItem;

        public GalleryItemHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.gallery_thumbnail_image);
        }

        public void bindItem(GalleryItem item) {
            mGalleryItem = item;
            mImageView.setOnClickListener(mItemClickListener);
            mImageView.setTag(mGalleryItem);
            if (WikipediaApp.getInstance().isImageDownloadEnabled()
                    && !TextUtils.isEmpty(mGalleryItem.getThumbUrl())) {
                Picasso.with(mContext)
                        .load(mGalleryItem.getThumbUrl())
                        .placeholder(R.drawable.checkerboard)
                        .error(R.drawable.checkerboard)
                        .into(mImageView);
            }
        }
    }

    private final class GalleryViewAdapter extends RecyclerView.Adapter<GalleryItemHolder> {
        @NonNull private final GalleryCollection mCollection;

        public GalleryViewAdapter(@NonNull GalleryCollection collection) {
            mCollection = collection;
        }

        @Override
        public int getItemCount() {
            return mCollection.getItemList().size();
        }

        @Override
        public GalleryItemHolder onCreateViewHolder(ViewGroup parent, int pos) {
            View view = LayoutInflater.from(mContext)
                    .inflate(R.layout.item_gallery_thumbnail, parent, false);
            return new GalleryItemHolder(view);
        }

        @Override
        public void onBindViewHolder(GalleryItemHolder holder, int pos) {
            holder.bindItem(mCollection.getItemList().get(pos));
        }
    }
}

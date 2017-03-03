package org.wikipedia.readinglist;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.R;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ViewUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.wikipedia.readinglist.ReadingLists.SORT_BY_NAME_ASC;
import static org.wikipedia.readinglist.ReadingLists.SORT_BY_NAME_DESC;
import static org.wikipedia.readinglist.ReadingLists.SORT_BY_RECENT_ASC;
import static org.wikipedia.readinglist.ReadingLists.SORT_BY_RECENT_DESC;

public class ReadingListDetailView extends LinearLayout {
    @BindView(R.id.reading_list_title) TextView titleView;
    @BindView(R.id.reading_list_count) TextView countView;
    @BindView(R.id.reading_list_description) GoneIfEmptyTextView descriptionView;
    @BindView(R.id.contents_list) RecyclerView contentsListView;

    @Nullable private ReadingList readingList;
    @NonNull private List<ReadingListPage> displayedPages = new ArrayList<>();
    @Nullable private Callback callback;
    private String currentSearchQuery;

    private ReadingListPageItemAdapter adapter = new ReadingListPageItemAdapter();
    private Bitmap deleteIcon = getDeleteBitmap();

    public interface Callback {
        void onClick(ReadingList readingList, ReadingListPage page);
        void onLongClick(ReadingList readingList, ReadingListPage page);
        void onDelete(ReadingList readingList, ReadingListPage page);
        void onBackPressed();
    }

    public ReadingListDetailView(Context context) {
        super(context);
        init();
    }

    public ReadingListDetailView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ReadingListDetailView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReadingListDetailView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setReadingListInfo(@NonNull ReadingList readingList, @NonNull List<String> otherTitles) {
        this.readingList = readingList;
        contentsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        contentsListView.setAdapter(adapter);
        updateDetails();
        getThumbnails();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void updateDetails() {
        if (readingList == null) {
            return;
        }
        titleView.setText(TextUtils.isEmpty(readingList.getTitle())
                ? getResources().getString(R.string.reading_list_untitled)
                : readingList.getTitle());
        countView.setText(readingList.getPages().size() == 1
                ? getResources().getString(R.string.reading_list_item_count_singular)
                : String.format(getResources().getString(R.string.reading_list_item_count_plural), readingList.getPages().size()));
        descriptionView.setText(readingList.getDescription());
        setSearchQuery(currentSearchQuery);
        updateSort();
    }

    public void updateSort() {
        sortPages(Prefs.getReadingListPageSortMode(SORT_BY_NAME_ASC));
        adapter.notifyDataSetChanged();
    }

    public void setSearchQuery(@Nullable String query) {
        currentSearchQuery = query;
        displayedPages.clear();
        adapter.notifyDataSetChanged();
        if (readingList == null) {
            return;
        }
        if (TextUtils.isEmpty(query)) {
            displayedPages.addAll(readingList.getPages());
            return;
        }
        query = query.toUpperCase();
        for (ReadingListPage page : readingList.getPages()) {
            if (page.title().toUpperCase().contains(query.toUpperCase())) {
                displayedPages.add(page);
            }
        }
    }

    @OnClick(R.id.reading_list_detail_back_button) void onBackPressed(View v) {
        if (callback != null) {
            callback.onBackPressed();
        }
    }

    private void init() {
        inflate(getContext(), R.layout.item_reading_list_detail, this);
        ButterKnife.bind(this);

        setLayoutParams(new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ItemTouchHelper.Callback touchCallback = new ReadingListItemTouchHelperCallback();
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(touchCallback);
        itemTouchHelper.attachToRecyclerView(contentsListView);
    }

    private void getThumbnails() {
        ReadingListImageFetcher.getThumbnails(readingList, new ReadingListImageFetcher.CompleteListener() {
            @Override
            public void onComplete() {
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Throwable e) {
            }
        });
    }

    private void sortPages(int sortMode) {
        switch (sortMode) {
            case SORT_BY_NAME_ASC:
                Collections.sort(displayedPages, new Comparator<ReadingListPage>() {
                    @Override
                    public int compare(ReadingListPage lhs, ReadingListPage rhs) {
                        return lhs.title().compareTo(rhs.title());
                    }
                });
                break;
            case SORT_BY_NAME_DESC:
                Collections.sort(displayedPages, new Comparator<ReadingListPage>() {
                    @Override
                    public int compare(ReadingListPage lhs, ReadingListPage rhs) {
                        return rhs.title().compareTo(lhs.title());
                    }
                });
                break;
            case SORT_BY_RECENT_ASC:
                Collections.sort(displayedPages, new Comparator<ReadingListPage>() {
                    @Override
                    public int compare(ReadingListPage lhs, ReadingListPage rhs) {
                        return ((Long) lhs.atime()).compareTo(rhs.atime());
                    }
                });
                break;
            case SORT_BY_RECENT_DESC:
                Collections.sort(displayedPages, new Comparator<ReadingListPage>() {
                    @Override
                    public int compare(ReadingListPage lhs, ReadingListPage rhs) {
                        return ((Long) rhs.atime()).compareTo(lhs.atime());
                    }
                });
                break;
            default:
                break;
        }
    }

    private class ReadingListPageItemHolder extends RecyclerView.ViewHolder implements OnClickListener, OnLongClickListener {
        private ReadingListPage page;
        private View containerView;
        private TextView titleView;
        private SimpleDraweeView thumbnailView;
        private GoneIfEmptyTextView descriptionView;

        ReadingListPageItemHolder(View itemView) {
            super(itemView);
            containerView = itemView.findViewById(R.id.page_list_item_container);
            titleView = (TextView) itemView.findViewById(R.id.page_list_item_title);
            descriptionView = (GoneIfEmptyTextView) itemView.findViewById(R.id.page_list_item_description);
            thumbnailView = (SimpleDraweeView) itemView.findViewById(R.id.page_list_item_image);
            containerView.setClickable(true);
            containerView.setOnClickListener(this);
        }

        public void bindItem(ReadingListPage page) {
            this.page = page;
            titleView.setText(page.title());
            descriptionView.setText(page.description());
            ViewUtil.loadImageUrlInto(thumbnailView, page.thumbnailUrl());
        }

        @Override
        public void onClick(View v) {
            if (callback != null) {
                callback.onClick(readingList, page);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (callback != null) {
                callback.onLongClick(readingList, page);
            }
            return true;
        }

        public void onDismiss() {
            if (callback != null) {
                callback.onDelete(readingList, page);
            }
            updateDetails();
        }
    }

    private final class ReadingListPageItemAdapter extends RecyclerView.Adapter<ReadingListPageItemHolder> {
        @Override
        public int getItemCount() {
            return displayedPages.size();
        }

        @Override
        public ReadingListPageItemHolder onCreateViewHolder(ViewGroup parent, int pos) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_page_list_entry, parent, false);
            return new ReadingListPageItemHolder(view);
        }

        @Override
        public void onBindViewHolder(ReadingListPageItemHolder holder, int pos) {
            holder.bindItem(displayedPages.get(pos));
        }
    }

    private class ReadingListItemTouchHelperCallback extends ItemTouchHelper.Callback {
        private static final float DELETE_ICON_PADDING_DP = 16f;
        private Paint deleteBackgroundPaint = new Paint();
        private Paint deleteIconPaint = new Paint();
        private Paint itemBackgroundPaint = new Paint();

        ReadingListItemTouchHelperCallback() {
            deleteBackgroundPaint.setStyle(Paint.Style.FILL);
            deleteBackgroundPaint.setColor(Color.RED);
            itemBackgroundPaint.setStyle(Paint.Style.FILL);
            itemBackgroundPaint.setColor(ContextCompat.getColor(getContext(),
                    ResourceUtil.getThemedAttributeId(getContext(), R.attr.window_background_color)));
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return true;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            final int dragFlags = 0; //ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            final int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            if (source.getItemViewType() != target.getItemViewType()) {
                return false;
            }
            // Notify the adapter of the move
            //adapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
            // Notify the adapter of the dismissal
            //adapter.onItemDismiss(viewHolder.getAdapterPosition());
            if (viewHolder instanceof ReadingListPageItemHolder) {
                // Let the view holder know that this item is being moved or dragged
                ReadingListPageItemHolder itemViewHolder = (ReadingListPageItemHolder) viewHolder;
                itemViewHolder.onDismiss();
            }
        }

        @Override
        public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dx, float dy, int actionState, boolean isCurrentlyActive) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                canvas.drawRect(0f, viewHolder.itemView.getTop(), viewHolder.itemView.getWidth(), viewHolder.itemView.getTop() + viewHolder.itemView.getHeight(), deleteBackgroundPaint);
                canvas.drawRect(dx, viewHolder.itemView.getTop(), viewHolder.itemView.getWidth() + dx, viewHolder.itemView.getTop() + viewHolder.itemView.getHeight(), itemBackgroundPaint);
                if (dx >= 0) {
                    canvas.drawBitmap(deleteIcon, DELETE_ICON_PADDING_DP * DimenUtil.getDensityScalar(), viewHolder.itemView.getTop() + (viewHolder.itemView.getHeight() / 2 - deleteIcon.getHeight() / 2), deleteIconPaint);
                } else {
                    canvas.drawBitmap(deleteIcon, viewHolder.itemView.getRight() - deleteIcon.getWidth() - DELETE_ICON_PADDING_DP * DimenUtil.getDensityScalar(), viewHolder.itemView.getTop() + (viewHolder.itemView.getHeight() / 2 - deleteIcon.getHeight() / 2), deleteIconPaint);
                }
                viewHolder.itemView.setTranslationX(dx);
            } else {
                super.onChildDraw(canvas, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive);
            }
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            // We only want the active item to change
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                if (viewHolder instanceof ReadingListPageItemHolder) {
                    // Let the view holder know that this item is being moved or dragged
                    ReadingListPageItemHolder itemViewHolder = (ReadingListPageItemHolder) viewHolder;
                    //itemViewHolder.onItemSelected();
                }
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            if (viewHolder instanceof ReadingListPageItemHolder) {
                // Tell the view holder it's time to restore the idle state
                ReadingListPageItemHolder itemViewHolder = (ReadingListPageItemHolder) viewHolder;
                //itemViewHolder.onItemClear();
            }
        }
    }

    private Bitmap getDeleteBitmap() {
        Drawable vectorDrawable = VectorDrawableCompat.create(getResources(), R.drawable.ic_delete_white_24dp, null);
        int width = vectorDrawable.getIntrinsicWidth();
        int height = vectorDrawable.getIntrinsicHeight();
        vectorDrawable.setBounds(0, 0, width, height);
        Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        vectorDrawable.draw(canvas);
        return bm;
    }
}

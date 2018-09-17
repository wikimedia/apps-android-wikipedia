package org.wikipedia.main.floatingqueue;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageBackStackItem;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.settings.Prefs;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FloatingQueueView extends FrameLayout {

    // TODO: convert this file to Kotlin

    public interface Callback {
        void onFloatingQueueClicked(@NonNull PageTitle title);
    }

    @BindView(R.id.floating_queue_thumbnail) FloatingQueueImageView floatingQueueThumbnail;
    @BindView(R.id.floating_queue_article) TextView floatingQueueArticle;
    @BindView(R.id.floating_queue_counts) TextView floatingQueueCounts;

    private static final float TAB_COUNT_TEXT_SIZE_DEFAULT = 12;
    private static final float TAB_COUNT_TEXT_SIZE_SMALL = 8;
    private static final int ANIMATION_DELAY_MILLIS = 300;
    private Callback callback;
    private boolean openPageFromFloatingQueue;

    public FloatingQueueView(Context context) {
        super(context);
        init();
    }

    public FloatingQueueView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FloatingQueueView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FloatingQueueImageView getImageView() {
        return floatingQueueThumbnail;
    }

    public void setCallback(@NonNull Callback callback) {
        this.callback = callback;
    }

    @SuppressWarnings("checkstyle:magicnumber")
    public void update() {
        openPageFromFloatingQueue = false;

        List<Tab> tabList = WikipediaApp.getInstance().getTabList();
        boolean shouldShowFloatingQueue = tabList.size() > 0;

        if (shouldShowFloatingQueue) {
            List<PageBackStackItem> backStackItems = tabList.get(tabList.size() - 1).getBackStack();
            if (backStackItems.size() > 0) {
                PageTitle title = backStackItems.get(backStackItems.size() - 1).getTitle();

                floatingQueueArticle.setText(title.getDisplayText());

                boolean shouldShowImage = shouldShowImage(title);
                // This fix the invisible issue when returning back from the PageActivity
                floatingQueueThumbnail.getImage().setLegacyVisibilityHandlingEnabled(true);

                // Prevent blink
                String imageUrl = Prefs.getFloatingQueueImage() == null ? title.getThumbUrl() : Prefs.getFloatingQueueImage();
                if ((floatingQueueThumbnail.getTag() == null || !floatingQueueThumbnail.getTag().equals(imageUrl))) {
                    floatingQueueThumbnail.load(shouldShowImage ? imageUrl : null);
                    floatingQueueThumbnail.setTag(imageUrl);
                }

                floatingQueueCounts.setText(String.valueOf(tabList.size()));
                floatingQueueCounts.setTextSize(TypedValue.COMPLEX_UNIT_SP, tabList.size() > 99
                        ? TAB_COUNT_TEXT_SIZE_SMALL
                        : TAB_COUNT_TEXT_SIZE_DEFAULT);

                setOnClickListener((v) -> {
                    openPageFromFloatingQueue = true;
                    callback.onFloatingQueueClicked(title);
                });
                animation(false);
            } else {
                shouldShowFloatingQueue = false;
            }
        }

        setVisibility(shouldShowFloatingQueue ? VISIBLE : INVISIBLE);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    public void animation(boolean isOnPause) {
        if (isOnPause) {
            floatingQueueArticle.animate().translationX(-floatingQueueArticle.getWidth());
            if (!openPageFromFloatingQueue) {
                floatingQueueThumbnail.animate().alpha(0.0f).setStartDelay(ANIMATION_DELAY_MILLIS);
            }
        } else {
            floatingQueueArticle.animate().translationX(0);
            if (!openPageFromFloatingQueue) {
                floatingQueueThumbnail.animate().alpha(1.0f).setDuration(ANIMATION_DELAY_MILLIS);
            }
        }
    }

    private boolean shouldShowImage(PageTitle pageTitle) {
        return !pageTitle.isMainPage() && !pageTitle.isFilePage()
                && pageTitle.getThumbUrl() != null && !pageTitle.getThumbUrl().isEmpty();
    }

    private void init() {
        inflate(getContext(), R.layout.view_floating_queue, this);
        ButterKnife.bind(this);
    }
}

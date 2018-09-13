package org.wikipedia.main.floatingqueue;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageBackStackItem;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.TabCountsView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FloatingQueueView extends FrameLayout {

    public interface Callback {
        void onFloatingQueueClicked(@NonNull PageTitle title);
    }

    @BindView(R.id.floating_queue_thumbnail) FaceAndColorDetectImageView floatingQueueThumbnail;
    @BindView(R.id.floating_queue_article) TextView floatingQueueArticle;
    @BindView(R.id.floating_queue_counts) TabCountsView floatingQueueCounts;

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

    public FaceAndColorDetectImageView getImageView() {
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

                // This fix the invisible issue when returning back from the PageActivity
                floatingQueueThumbnail.setLegacyVisibilityHandlingEnabled(true);

                // Prevent blink
                String imageUrl = Prefs.getFloatingQueueImage() == null ? title.getThumbUrl() : Prefs.getFloatingQueueImage();
                if ((floatingQueueThumbnail.getTag() == null || !floatingQueueThumbnail.getTag().equals(imageUrl))) {
                    floatingQueueThumbnail.loadImage(!TextUtils.isEmpty(imageUrl) ? Uri.parse(imageUrl) : null);
                    floatingQueueThumbnail.setTag(imageUrl);
                }

                floatingQueueCounts.setTabSize(tabList.size());

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

    private void init() {
        inflate(getContext(), R.layout.view_floating_queue, this);
        ButterKnife.bind(this);

        // TODO: remove as soon as we drop support for API 19, and replace with CardView with elevation.
        setBackgroundResource(ResourceUtil.getThemedAttributeId(getContext(), R.attr.shadow_background_drawable));
    }
}

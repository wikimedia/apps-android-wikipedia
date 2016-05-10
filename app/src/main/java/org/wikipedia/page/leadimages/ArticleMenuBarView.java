package org.wikipedia.page.leadimages;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.wikipedia.R;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ArticleMenuBarView extends LinearLayout {
    public interface Callback {
        void onBookmarkClick();
        void onShareClick();
        void onNavigateClick();
    }

    public static class DefaultCallback implements Callback {
        @Override public void onBookmarkClick() { }
        @Override public void onShareClick() { }
        @Override public void onNavigateClick() { }
    }

    @BindView(R.id.view_article_menu_bar_bookmark) ImageView bookmark;
    @BindView(R.id.view_article_menu_bar_navigate) ImageView navigate;
    @BindView(R.id.view_article_menu_bar_share) ImageView share;

    @NonNull private Callback callback = new DefaultCallback();

    public ArticleMenuBarView(Context context) {
        super(context);
        init();
    }

    public ArticleMenuBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ArticleMenuBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ArticleMenuBarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback == null ? new DefaultCallback() : callback;
    }

    public void updateBookmark(boolean bookmarkSaved) {
        bookmark.setImageResource(bookmarkSaved ? R.drawable.ic_bookmark_black_24dp
                : R.drawable.ic_bookmark_border_black_24dp);
    }

    public void updateNavigate(boolean geolocated) {
        navigate.setVisibility(geolocated ? VISIBLE : GONE);
    }

    public void resetMenuBarColor() {
        setMenuBarColor(getResources().getColor(R.color.grey_700));
    }

    public void setMenuBarColor(@ColorInt int color) {
        final int animDuration = 500;
        final ObjectAnimator animator = ObjectAnimator.ofObject(getBackground(), "color",
                new ArgbEvaluator(), color);
        animator.setDuration(animDuration);
        animator.start();
    }

    @OnClick({R.id.view_article_menu_bar_bookmark,
              R.id.view_article_menu_bar_share,
              R.id.view_article_menu_bar_navigate})
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.view_article_menu_bar_bookmark:
                callback.onBookmarkClick();
                break;
            case R.id.view_article_menu_bar_share:
                callback.onShareClick();
                break;
            case R.id.view_article_menu_bar_navigate:
                callback.onNavigateClick();
                break;
            default:
                L.w("Unknown id=" + StringUtil.intToHexStr(view.getId()));
                break;
        }
    }

    private void init() {
        inflate();
        bind();
        FeedbackUtil.setToolbarButtonLongPressToast(bookmark, navigate, share);
    }

    private void inflate() {
        inflate(getContext(), R.layout.view_article_menu_bar, this);
    }

    private void bind() {
        ButterKnife.bind(this);
    }
}

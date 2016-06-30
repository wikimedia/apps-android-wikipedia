package org.wikipedia.page.leadimages;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.wikipedia.R;
import org.wikipedia.media.AvPlayer;
import org.wikipedia.media.DefaultAvPlayer;
import org.wikipedia.media.MediaPlayerImplementation;
import org.wikipedia.richtext.LeadingSpan;
import org.wikipedia.richtext.ParagraphSpan;
import org.wikipedia.richtext.AudioUrlSpan;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.AppTextView;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.wikipedia.util.ResourceUtil.getThemedAttributeId;
import static org.wikipedia.util.DimenUtil.leadImageHeightForDevice;

public class ArticleHeaderView extends FrameLayout implements ObservableWebView.OnScrollChangeListener {
    @BindView(R.id.view_article_header_image) ArticleHeaderImageView image;
    @BindView(R.id.view_article_header_text)
    AppTextView text;
    @BindView(R.id.view_article_header_menu_bar) ArticleMenuBarView menuBar;

    @NonNull private CharSequence title = "";
    @NonNull private CharSequence subtitle = "";
    @Nullable private String pronunciationUrl;

    @NonNull private final AvPlayer avPlayer = new DefaultAvPlayer(new MediaPlayerImplementation());

    public ArticleHeaderView(Context context) {
        super(context);
        init();
    }

    public ArticleHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ArticleHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ArticleHeaderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void hide() {
        setVisibility(View.GONE);
    }

    public void showText() {
        setVisibility(View.VISIBLE);

        updateText();

        setTextColor(getColor(getThemedAttributeId(getContext(),
                R.attr.lead_disabled_text_color)));
        setTextHeightUnconstrained();
        clearTextDropShadow();
        clearTextGradient();
    }

    public void showTextImage() {
        setVisibility(View.VISIBLE);

        updateText();

        setTextColor(getColor(R.color.lead_text_color));
        setImageHeight(leadImageHeightForDevice());
        setTextHeightConstrained();
        setTextDropShadow();
        setTextGradient();
    }

    // TODO: remove.
    public ImageView getImage() {
        return image.getImage();
    }

    public void setOnImageLoadListener(@Nullable FaceAndColorDetectImageView.OnImageLoadListener listener) {
        image.setLoadListener(listener);
    }

    public void loadImage(@Nullable String url) {
        image.load(url);
        int height = url == null ? 0 : leadImageHeightForDevice();
        setMinimumHeight(height);
        if (url == null) {
            resetMenuBarColor();
        }
    }

    public boolean hasImage() {
        return image.hasImage();
    }

    public void setAnimationPaused(boolean paused) {
        image.setAnimationPaused(paused);
    }

    public Bitmap copyBitmap() {
        return ViewUtil.getBitmapFromView(image);
    }

    public void setImageYScalar(float offset) {
        image.setFocusOffset(offset);
        updateParallaxScroll();
    }

    public int getLineCount() {
        return text.getLineCount();
    }

    public CharSequence getText() {
        return text.getText();
    }

    public void setTitle(@Nullable CharSequence text) {
        title = StringUtil.emptyIfNull(text);
        updateText();
    }

    public void setSubtitle(@Nullable CharSequence text) {
        subtitle = StringUtil.emptyIfNull(text);
        updateText();
    }

    public boolean hasSubtitle() {
        return !TextUtils.isEmpty(subtitle);
    }

    public void setLocale(String locale) {
        text.setLocale(locale);
    }

    public void setTextColor(@ColorInt int color) {
        text.setTextColor(color);
    }

    public void setPronunciation(@Nullable String url) {
        pronunciationUrl = url;
        updateText();
    }

    public boolean hasPronunciation() {
        return pronunciationUrl != null;
    }

    public void updateBookmark(boolean bookmarkSaved) {
        menuBar.updateBookmark(bookmarkSaved);
    }

    public void updateNavigate(boolean geoLocated) {
        menuBar.updateNavigate(geoLocated);
    }

    public void resetMenuBarColor() {
        menuBar.resetMenuBarColor();
    }

    public void setMenuBarColor(@ColorInt int color) {
        menuBar.setMenuBarColor(color);
    }

    public void setMenuBarCallback(@Nullable ArticleMenuBarView.Callback callback) {
        menuBar.setCallback(callback);
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY, boolean isHumanScroll) {
        updateParallaxScroll(scrollY);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        avPlayer.init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateParallaxScroll();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        avPlayer.deinit();
    }

    private void updateParallaxScroll() {
        updateParallaxScroll((int) -getTranslationY());
    }

    private void updateParallaxScroll(int scrollY) {
        int offset = Math.min(getHeight(), scrollY);
        setTranslationY(-offset);
        image.setTranslationY(offset / 2);
    }

    private void updateText() {
        avPlayer.stop();

        SpannableStringBuilder builder = new SpannableStringBuilder(title);

        if (hasPronunciation()) {
            builder.append(" ");
            builder.append(pronunciationSpanned());
        }

        if (hasSubtitle()) {
            builder.append("\n");
            builder.append(subtitleSpanned());
        }

        text.setText(builder);
    }

    private Spanned pronunciationSpanned() {
        AudioUrlSpan span = new AudioUrlSpan(text, avPlayer, pronunciationUrl,
                AudioUrlSpan.ALIGN_BASELINE);
        span.setTint(hasImage() ? Color.WHITE : getColor(getThemedAttributeId(getContext(), R.attr.window_inverse_color)));
        return RichTextUtil.setSpans(new SpannableString(" "),
                0,
                1,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE,
                span);
    }

    private Spanned subtitleSpanned() {
        final float leadingScalar = DimenUtil.getFloat(R.dimen.lead_subtitle_leading_scalar);
        final float paragraphScalar = DimenUtil.getFloat(R.dimen.lead_subtitle_paragraph_scalar);
        return RichTextUtil.setSpans(new SpannableString(subtitle),
                0,
                subtitle.length(),
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE,
                new AbsoluteSizeSpan(getDimensionPixelSize(R.dimen.descriptionTextSize),
                        false),
                new LeadingSpan(leadingScalar),
                new ParagraphSpan(paragraphScalar));
    }

    private void setTextDropShadow() {
        text.setShadowLayer(2, 1, 1, getColor(R.color.lead_text_shadow));
    }

    private void clearTextDropShadow() {
        text.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
    }

    private void clearTextGradient() {
        text.setBackgroundColor(Color.TRANSPARENT);
    }

    private void setTextGradient() {
        Drawable gradient = GradientUtil.getCubicGradient(getColor(R.color.lead_gradient_start),
                Gravity.BOTTOM);
        ViewUtil.setBackgroundDrawable(text, gradient);
    }

    private void setImageHeight(int height) {
        DimenUtil.setViewHeight(image, height);
    }

    private void setTextHeightConstrained() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        text.setLayoutParams(params);
    }

    private void setTextHeightUnconstrained() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        text.setLayoutParams(params);
    }

    private void init() {
        inflate();
        bind();
        initText();
        hide();
    }

    private void inflate() {
        inflate(getContext(), R.layout.view_article_header, this);
    }

    private void bind() {
        ButterKnife.bind(this);
    }

    private void initText() {
        // TODO: replace with android:fontFamily="serif" attribute when our minimum API level is
        //       Jelly Bean, API 16, or if we make custom typeface attribute.
        text.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
    }

    @ColorInt
    private int getColor(@ColorRes int id) {
        return ContextCompat.getColor(getContext(), id);
    }

    private int getDimensionPixelSize(@DimenRes int id) {
        return getResources().getDimensionPixelSize(id);
    }
}

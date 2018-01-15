package org.wikipedia.page.leadimages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.PopupMenu;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.wikipedia.R;
import org.wikipedia.media.AvPlayer;
import org.wikipedia.media.DefaultAvPlayer;
import org.wikipedia.media.MediaPlayerImplementation;
import org.wikipedia.richtext.AudioUrlSpan;
import org.wikipedia.richtext.LeadingSpan;
import org.wikipedia.richtext.ParagraphSpan;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.AppTextView;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.LinearLayoutOverWebView;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.wikipedia.util.DimenUtil.leadImageHeightForDevice;
import static org.wikipedia.util.L10nUtil.isLangRTL;
import static org.wikipedia.util.ResourceUtil.getThemedColor;

public class PageHeaderView extends LinearLayoutOverWebView implements ObservableWebView.OnScrollChangeListener {
    @BindView(R.id.view_page_header_image) PageHeaderImageView image;
    @BindView(R.id.view_page_title_text) AppTextView titleText;
    @BindView(R.id.view_page_subtitle_text) AppTextView subtitleText;
    @BindView(R.id.view_page_header_divider) View divider;
    @BindView(R.id.view_page_header_edit_pencil) ImageView editPencil;
    @BindView(R.id.view_page_subtitle_container) LinearLayout subtitleContainer;

    @Nullable private Callback callback;
    @VisibleForTesting @NonNull CharSequence title = "";
    @VisibleForTesting @NonNull CharSequence subtitle = "";
    @VisibleForTesting @Nullable String pronunciationUrl;
    private boolean allowDescriptionEdit;

    @NonNull private final AvPlayer avPlayer = new DefaultAvPlayer(new MediaPlayerImplementation());
    @VisibleForTesting @NonNull final ClickableSpan descriptionClickSpan = new DescriptionClickableSpan();

    public interface Callback {
        void onImageClicked();
        void onDescriptionClicked();
        void onEditDescription();
        void onEditLeadSection();
    }

    public PageHeaderView(Context context) {
        super(context);
        init();
    }

    public PageHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PageHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void hide() {
        setVisibility(View.GONE);
    }

    public void showText() {
        setVisibility(View.VISIBLE);
        setTopOffset();
        updateText();
    }

    public void showTextImage() {
        setVisibility(View.VISIBLE);
        unsetTopOffset();
        updateText();
        DimenUtil.setViewHeight(image, leadImageHeightForDevice());
    }

    // TODO: remove.
    @NonNull public ImageView getImage() {
        return image.getImage();
    }

    public void setOnImageLoadListener(@Nullable FaceAndColorDetectImageView.OnImageLoadListener listener) {
        image.setLoadListener(listener);
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void loadImage(@Nullable String url) {
        image.load(url);
        int height = url == null ? 0 : leadImageHeightForDevice();
        setMinimumHeight(height);
    }

    public void setAnimationPaused(boolean paused) {
        image.setAnimationPaused(paused);
    }

    @NonNull
    public Bitmap copyBitmap() {
        return ViewUtil.getBitmapFromView(image.getImage());
    }

    public void setImageFocus(PointF focusPoint) {
        image.setFocusPoint(focusPoint);
        updateScroll();
    }

    public void setTitle(@Nullable CharSequence text) {
        title = defaultIfEmpty(text, "");
        updateText();
    }

    public void setSubtitle(@Nullable CharSequence text) {
        subtitle = defaultIfEmpty(text, "");
        updateText();
    }

    public boolean hasSubtitle() {
        return !TextUtils.isEmpty(subtitle);
    }

    public void setProtected(boolean isProtected) {
        editPencil.setImageResource(isProtected
                ? R.drawable.ic_edit_lock_24px : R.drawable.ic_mode_edit_white_24dp);
    }

    public void setLocale(@NonNull String locale) {
        titleText.setLocale(locale);
        subtitleText.setLocale(locale);
        LinearLayout.LayoutParams dividerParams = (LinearLayout.LayoutParams) divider.getLayoutParams();
        dividerParams.gravity = isLangRTL(locale) ? Gravity.RIGHT : Gravity.LEFT;
        divider.setLayoutParams(dividerParams);
        LinearLayout.LayoutParams pencilParams = (LinearLayout.LayoutParams) editPencil.getLayoutParams();
        pencilParams.gravity = Gravity.BOTTOM | (isLangRTL(locale) ? Gravity.LEFT : Gravity.RIGHT);
        subtitleContainer.setLayoutDirection(isLangRTL(locale) ? LAYOUT_DIRECTION_RTL : LAYOUT_DIRECTION_LTR);

        int subtitlePadding = editPencil.getWidth();
        subtitleText.setPadding(isLangRTL(locale) ? subtitlePadding : 0,
                subtitleText.getPaddingTop(),
                isLangRTL(locale) ? 0 : subtitlePadding,
                subtitleText.getPaddingBottom());

        if (TextUtils.isEmpty(subtitle)) {
            subtitleText.setCompoundDrawablesWithIntrinsicBounds(
                    isLangRTL(locale) ? 0 : R.drawable.ic_short_text,
                    0,
                    isLangRTL(locale) ? R.drawable.ic_short_text : 0,
                    0);
        } else {
            subtitleText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    public void setPronunciation(@Nullable String url) {
        pronunciationUrl = url;
        updateText();
    }

    public boolean hasPronunciation() {
        return pronunciationUrl != null;
    }

    public void setAllowDescriptionEdit(boolean allow) {
        allowDescriptionEdit = allow;
        updateText();
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY, boolean isHumanScroll) {
        updateScroll(scrollY);
    }

    @OnClick(R.id.view_page_header_image) void onImageClick() {
        if (callback != null) {
            callback.onImageClicked();
        }
    }

    @OnClick(R.id.view_page_header_edit_pencil) void onEditClick() {
        if (allowDescriptionEdit) {
            PopupMenu menu = new PopupMenu(editPencil.getContext(), editPencil, Gravity.END, 0,
                    R.style.PagePopupMenu);
            menu.getMenuInflater().inflate(R.menu.menu_page_header_edit, menu.getMenu());
            menu.setOnMenuItemClickListener(new EditMenuClickListener());
            menu.show();
        } else {
            if (callback != null) {
                callback.onEditLeadSection();
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        avPlayer.init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateScroll();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        avPlayer.deinit();
    }

    private void updateScroll() {
        updateScroll((int) -getTranslationY());
    }

    private void updateScroll(int scrollY) {
        int offset = Math.min(getHeight(), scrollY);
        image.getImage().setTranslationY(offset / 2);
        setTranslationY(-offset);
    }

    private void updateText() {
        avPlayer.stop();

        SpannableStringBuilder builder = new SpannableStringBuilder(title);
        builder.setSpan(new TypefaceSpan("serif"), 0, title.length(),
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

        if (hasPronunciation()) {
            builder.append(" ");
            builder.append(pronunciationSpanned());
        }
        titleText.setMovementMethod(new LinkMovementMethod());
        titleText.setText(builder);

        if (hasSubtitle() || allowDescriptionEdit) {
            subtitleText.setMovementMethod(hasSubtitle() ? null : new LinkMovementMethod());
            subtitleText.setText(subtitleSpanned());
            subtitleText.setVisibility(VISIBLE);
        } else {
            subtitleText.setVisibility(INVISIBLE);
        }
    }

    private Spanned pronunciationSpanned() {
        AudioUrlSpan pronunciationSpan = new AudioUrlSpan(titleText, avPlayer, pronunciationUrl,
                AudioUrlSpan.ALIGN_BASELINE);
        pronunciationSpan.setTint(getThemedColor(getContext(), R.attr.primary_text_color));
        return RichTextUtil.setSpans(new SpannableString("  "),
                0,
                1,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE,
                pronunciationSpan,
                new PronunciationClickableSpan(pronunciationSpan));
    }

    private Spanned subtitleSpanned() {
        final float leadingScalar = DimenUtil.getFloat(R.dimen.lead_subtitle_leading_scalar);
        final float paragraphScalar = DimenUtil.getFloat(R.dimen.lead_subtitle_paragraph_scalar);
        String description = TextUtils.isEmpty(subtitle)
                ? getResources().getString(R.string.description_edit_add_description)
                : subtitle.toString();
        return RichTextUtil.setSpans(new SpannableString(description),
                0,
                description.length(),
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE,
                new LeadingSpan(leadingScalar),
                new ParagraphSpan(paragraphScalar),
                TextUtils.isEmpty(subtitle) ? descriptionClickSpan
                        : new ForegroundColorSpan(ResourceUtil.getThemedColor(getContext(), R.attr.secondary_text_color)),
                TextUtils.isEmpty(subtitle) ? new StyleSpan(Typeface.ITALIC) : null);
    }

    private void init() {
        inflate(getContext(), R.layout.view_page_header, this);
        ButterKnife.bind(this);
        FeedbackUtil.setToolbarButtonLongPressToast(editPencil);
        hide();
    }

    private int getDimensionPixelSize(@DimenRes int id) {
        return getResources().getDimensionPixelSize(id);
    }

    private void setTopOffset() {
        setTopOffset(true);
    }

    private void unsetTopOffset() {
        setTopOffset(false);
    }

    private void setTopOffset(boolean noImage) {
        int offset = noImage ? getDimensionPixelSize(R.dimen.lead_no_image_top_offset_dp) : 0;

        // Offset is a resolved pixel dimension, not a resource id
        //noinspection ResourceType
        setPadding(0, offset, 0, 0);
    }

    private class DescriptionClickableSpan extends ClickableSpan {
        @Override
        public void onClick(View view) {
            if (callback != null) {
                callback.onDescriptionClicked();
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setColor(ResourceUtil.getThemedColor(getContext(), TextUtils.isEmpty(subtitle)
                    ? R.attr.colorAccent : R.attr.secondary_text_color));
            ds.setUnderlineText(false);
        }
    }

    private class PronunciationClickableSpan extends ClickableSpan {
        @NonNull private AudioUrlSpan audioSpan;

        PronunciationClickableSpan(@NonNull AudioUrlSpan audioSpan) {
            this.audioSpan = audioSpan;
        }

        @Override
        public void onClick(View view) {
            audioSpan.toggle();
        }
    }

    private class EditMenuClickListener implements PopupMenu.OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_page_header_edit_description:
                    if (callback != null) {
                        callback.onEditDescription();
                    }
                    return true;
                case R.id.menu_page_header_edit_lead_section:
                    if (callback != null) {
                        callback.onEditLeadSection();
                    }
                    return true;
                default:
                    return false;
            }
        }
    }
}

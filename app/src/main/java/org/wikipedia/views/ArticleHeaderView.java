package org.wikipedia.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.page.leadimages.ImageViewWithFace;
import org.wikipedia.page.leadimages.ImageViewWithFace.OnImageLoadListener;
import org.wikipedia.util.GradientUtil;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ArticleHeaderView extends FrameLayout {
    @Bind(R.id.image) ImageViewWithFace image;
    @Bind(R.id.placeholder) ImageView placeholder;
    @Bind(R.id.text) AppTextView text;

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

        image.setVisibility(View.GONE);

        placeholder.setVisibility(View.GONE);

        setTextColor(getColor(Utils.getThemedAttributeId(getContext(),
                R.attr.lead_disabled_text_color)));
        clearTextDropShadow();
        clearTextGradient();
    }

    public void showTextImage() {
        setVisibility(View.VISIBLE);

        image.setVisibility(View.INVISIBLE);

        placeholder.setVisibility(View.VISIBLE);

        setTextColor(getColor(R.color.lead_text_color));
        setTextDropShadow();
        setTextGradient();
    }

    // TODO: remove.
    public ImageViewWithFace getImage() {
        return image;
    }

    public void setOnImageLoadListener(OnImageLoadListener listener) {
        image.setOnImageLoadListener(listener);
    }

    public void loadImage(@NonNull String url) {
        Picasso.with(getContext())
                .load(url)
                .noFade()
                .into((Target) image);
    }

    // ideas from:
    // http://stackoverflow.com/questions/2801116/converting-a-view-to-bitmap-without-displaying-it-in-android
    // View has to be already displayed. Note: a copy of the ImageView's Drawable must be made in
    // some fashion as it may be recycled. See T114658.
    public Bitmap copyImage() {
        // Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(),
                Bitmap.Config.ARGB_8888);
        // Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        image.draw(canvas);
        return returnedBitmap;
    }

    // TODO: remove.
    public ImageView getPlaceholder() {
        return placeholder;
    }

    public int getLineCount() {
        return text.getLineCount();
    }

    public CharSequence getText() {
        return text.getText();
    }

    public void setText(CharSequence text) {
        this.text.setText(text);
    }

    public void setText(CharSequence text, String locale) {
        this.text.setText(text, locale);
    }

    public void setTextColor(@ColorInt int color) {
        text.setTextColor(color);
    }

    public int getTextHeight() {
        return text.getHeight();
    }

    public void setTextSize(int unit, float size) {
        text.setTextSize(unit, size);
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
        //       Jelly Bean, API 16, or we make custom typeface attribute.
        text.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
    }

    @ColorInt private int getColor(@ColorRes int id) {
        return ContextCompat.getColor(getContext(), id);
    }
}
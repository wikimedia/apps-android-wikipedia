package org.wikipedia.edit.summaries;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.widget.LinearLayout.LayoutParams;

import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;

public class EditSummaryTag extends AppCompatTextView {
    public static final int MARGIN = 4;
    public static final int PADDING = 8;

    private Resources resources;
    private boolean selected = false;

    public EditSummaryTag(Context context) {
        super(context);
        setupEditSummaryTag(context);
    }

    public EditSummaryTag(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupEditSummaryTag(context);
    }

    public EditSummaryTag(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupEditSummaryTag(context);
    }

    private void setupEditSummaryTag(Context context) {
        resources = context.getResources();

        LayoutParams params = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );

        int margin = (int) DimenUtil.dpToPx(MARGIN);
        int padding = (int) DimenUtil.dpToPx(PADDING);
        params.setMargins(margin, margin, margin, margin);
        setLayoutParams(params);

        setPadding(padding, padding, padding, padding);

        setOnClickListener((view) -> {
            selected = !selected;
            updateState();
        });

        updateState();
    }

    @Override
    public String toString() {
        return getText().toString();
    }

    public boolean getSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
        updateState();
    }

    private void updateState() {
        @AttrRes int backgroundAttributeResource = selected
                ? R.attr.edit_improve_tag_selected_drawable : R.attr.edit_improve_tag_unselected_drawable;
        setBackgroundResource(ResourceUtil.getThemedAttributeId(getContext(), backgroundAttributeResource));

        @ColorInt int textColor = ResourcesCompat.getColor(resources, selected
                ? android.R.color.white : ResourceUtil.getThemedAttributeId(getContext(), R.attr.colorAccent), null);
        setTextColor(textColor);
    }
}

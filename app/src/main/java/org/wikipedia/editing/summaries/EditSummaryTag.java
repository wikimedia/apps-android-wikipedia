package org.wikipedia.editing.summaries;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;

public class EditSummaryTag extends TextView {
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

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                selected = !selected;
                updateState();
            }
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
        setBackgroundResource(selected ? R.drawable.editpage_improve_tag_selected : R.drawable.editpage_improve_tag_unselected);
        setTextColor(resources.getColor(selected ? android.R.color.white : R.color.foundation_blue));
    }
}

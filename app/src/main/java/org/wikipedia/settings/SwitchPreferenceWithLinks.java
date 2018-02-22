package org.wikipedia.settings;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.util.ResourceUtil;

import static org.wikipedia.util.UriUtil.handleExternalLink;

public class SwitchPreferenceWithLinks extends SwitchPreferenceCompat {

    private LinkMovementMethodExt movementMethod
            = new LinkMovementMethodExt((@NonNull String url, @Nullable String titleString) -> handleExternalLink(getContext(), Uri.parse(url)));

    private View.OnClickListener onClickListener = (v) -> {
        SwitchPreferenceWithLinks sw = SwitchPreferenceWithLinks.this;
        sw.setChecked(!sw.isChecked());
        sw.callChangeListener(sw.isChecked());
    };

    public SwitchPreferenceWithLinks(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
    }

    public SwitchPreferenceWithLinks(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
    }

    public SwitchPreferenceWithLinks(Context ctx) {
        super(ctx);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView view = (TextView) holder.findViewById(android.R.id.summary);
        view.setMovementMethod(movementMethod);
        view.setOnClickListener(onClickListener);
        view.setBackground(ContextCompat.getDrawable(getContext(),
                ResourceUtil.getThemedAttributeId(getContext(), R.attr.selectableItemBackgroundBorderless)));
    }
}

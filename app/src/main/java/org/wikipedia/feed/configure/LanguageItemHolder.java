package org.wikipedia.feed.configure;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.DefaultViewHolder;

public class LanguageItemHolder extends DefaultViewHolder<View> {
    private Context context;
    private TextView langCodeView;

    LanguageItemHolder(Context context, View itemView) {
        super(itemView);
        this.context = context;
        langCodeView = itemView.findViewById(R.id.feed_content_type_lang_code);
    }

    void bindItem(@NonNull String langCode, boolean enabled) {
        langCodeView.setText(langCode);
        langCodeView.setTextColor(enabled ? ContextCompat.getColor(context, android.R.color.white)
                : ResourceUtil.getThemedColor(context, R.attr.material_theme_de_emphasised_color));
        langCodeView.setBackground(ContextCompat.getDrawable(context,
                enabled ? R.drawable.lang_button_shape : R.drawable.lang_button_shape_border));
        langCodeView.getBackground().setColorFilter(enabled ? ContextCompat.getColor(context, R.color.base30)
                        : ResourceUtil.getThemedColor(context, R.attr.material_theme_de_emphasised_color),
                PorterDuff.Mode.SRC_IN);
    }
}

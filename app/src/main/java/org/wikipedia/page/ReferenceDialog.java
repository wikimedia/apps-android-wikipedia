package org.wikipedia.page;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.util.StringUtil;

/**
 * A dialog that displays the currently clicked reference.
 */
public class ReferenceDialog extends BottomSheetDialog {
    public ReferenceDialog(@NonNull Context context, @NonNull LinkHandler linkHandler,
                           @NonNull String html, @NonNull String linkText) {
        super(context);
        View rootView = LayoutInflater.from(context).inflate(R.layout.dialog_reference, null);
        setContentView(rootView);

        TextView referenceText = rootView.findViewById(R.id.reference_text);
        referenceText.setText(StringUtil.fromHtml(html));
        referenceText.setMovementMethod(new LinkMovementMethodExt(linkHandler));

        TextView titleText = rootView.findViewById(R.id.reference_title_text);
        titleText.setText(getContext().getString(R.string.reference_title, linkText));
    }
}

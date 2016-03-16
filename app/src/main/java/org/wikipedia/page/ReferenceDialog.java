package org.wikipedia.page;

import org.wikipedia.R;
import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * A dialog that displays the currently clicked reference.
 */
public class ReferenceDialog extends NoDimBottomSheetDialog {
    public ReferenceDialog(Context context, LinkHandler linkHandler, String html) {
        super(context);
        View rootView = LayoutInflater.from(context).inflate(R.layout.dialog_reference, null);
        setContentView(rootView);

        TextView referenceText = (TextView) rootView.findViewById(R.id.reference_text);
        referenceText.setText(Html.fromHtml(html));
        referenceText.setMovementMethod(new LinkMovementMethodExt(linkHandler));
    }
}

package org.wikipedia.page;

import org.wikipedia.R;
import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;

/**
 * A dialog that displays the currently clicked reference.
 */
public class ReferenceDialog extends BottomDialog {
    private final LinkHandler linkHandler;
    private final TextView referenceText;

    public ReferenceDialog(Context context, LinkHandler linkHandler) {
        super(context, R.layout.dialog_reference);
        this.linkHandler = linkHandler;
        referenceText = (TextView) getDialogLayout().findViewById(R.id.reference_text);
    }

    void updateReference(String refHtml) {
        Spanned html = Html.fromHtml(refHtml);
        referenceText.setText(html);
        referenceText.setMovementMethod(new LinkMovementMethodExt(linkHandler));
    }
}

package org.wikipedia.page;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import org.wikipedia.R;

/** Display the currently clicked reference */
public class ReferenceDialog extends Dialog {
    private final LinkHandler linkHandler;
    private final TextView referenceText;

    public ReferenceDialog(Context context, LinkHandler linkHandler) {
        super(context);
        this.linkHandler = linkHandler;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dlgLayout = inflater.inflate(R.layout.dialog_reference, null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getWindow().setDimAmount(0.0f);
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(dlgLayout);

        getWindow().setBackgroundDrawable(null);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        getWindow().setAttributes(lp);

        referenceText = (TextView) dlgLayout.findViewById(R.id.reference_text);
    }

    void updateReference(String refHtml) {
        Spanned html = Html.fromHtml(refHtml);
        referenceText.setText(html);
        referenceText.setMovementMethod(new LinkMovementMethodExt(linkHandler));
    }
}

package org.wikipedia.page;

import org.wikipedia.R;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.v7.app.AppCompatDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

/**
 * A dialog that appears at the bottom of the page.
 *
 * It slides up when the dialog is opened, and down when the dialog is closed.
 * Uses the R.style.BottomDialog style, which specifies the animation.
 */
public class BottomDialog extends AppCompatDialog {
    private View dialogLayout;

    public BottomDialog(Context context, @LayoutRes int dialogLayoutResId) {
        super(context, R.style.BottomDialog);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dialogLayout = inflater.inflate(dialogLayoutResId, null);
        setContentView(dialogLayout);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        getWindow().setAttributes(lp);
    }

    protected View getDialogLayout() {
        return dialogLayout;
    }
}

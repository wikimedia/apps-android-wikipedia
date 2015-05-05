package org.wikipedia.page;

import org.wikipedia.R;
import org.wikipedia.util.ApiUtil;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * A dialog that appears at the bottom of the page.
 *
 * It slides up when the dialog is opened, and down when the dialog is closed.
 * see R.style.DialogSlideAnim for the animation
 */
public class BottomDialog extends Dialog {
    private View dialogLayout;

    public BottomDialog(Context context, int dialogLayoutResId) {
        super(context, R.style.DialogSlideAnim);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dialogLayout = inflater.inflate(dialogLayoutResId, null);

        if (ApiUtil.hasIceCreamSandwich()) {
            getWindow().setDimAmount(0.0f);
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(dialogLayout);

        getWindow().setBackgroundDrawable(null);
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

package org.wikipedia.beta.views;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.ClipboardManager;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.EditText;

public class PlainPasteEditText extends EditText {
    private Context context;

    public PlainPasteEditText(Context context) {
        super(context);
        this.context = context;
    }

    public PlainPasteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public PlainPasteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        switch (id) {
            case android.R.id.cut:
                break;
            case android.R.id.paste:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    // Do not allow pasting of formatted text!
                    // We do this by intercepting the clipboard and temporarily replacing its
                    // contents with plain text.
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard.hasPrimaryClip()) {
                        ClipData oldClipData = clipboard.getPrimaryClip();
                        String lastClipText = oldClipData.getItemAt(oldClipData.getItemCount() - 1).coerceToText(context).toString();
                        // create a new text-only clip data
                        ClipData newClipData = new ClipData(new ClipDescription("text", new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}),
                                new ClipData.Item(lastClipText));
                        // temporarily set the new clip data as the primary
                        clipboard.setPrimaryClip(newClipData);
                        // execute the paste!
                        boolean ret = super.onTextContextMenuItem(id);
                        // restore the clip data back to the old one.
                        clipboard.setPrimaryClip(oldClipData);
                        return ret;
                    }
                }
                break;
            case android.R.id.copy:
                break;
            default:
        }
        return super.onTextContextMenuItem(id);
    }

}

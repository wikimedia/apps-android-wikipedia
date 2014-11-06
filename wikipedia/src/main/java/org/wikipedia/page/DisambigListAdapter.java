package org.wikipedia.page;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import android.app.Activity;
import android.text.Html;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 *
 */
class DisambigListAdapter extends ArrayAdapter<String> {
    private final Activity activity;
    private final String[] items;
    private LinkMovementMethodExt movementMethod;

    /**
     * Constructor
     * @param activity The current activity.
     * @param items The objects to represent in the ListView.
     */
    public DisambigListAdapter(Activity activity, String[] items, LinkMovementMethodExt movementMethod) {
        super(activity, 0, items);
        this.activity = activity;
        this.items = items;
        this.movementMethod = movementMethod;
    }

    class ViewHolder {
        private ImageView icon;
        private TextView text;
    }

    @Override
    public boolean isEnabled(int position) {
        return false; // don't make it appear clickable
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = activity.getLayoutInflater();
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_disambig, null);
            holder = new ViewHolder();
            holder.icon = (ImageView) convertView.findViewById(R.id.disambig_icon);
            holder.text = (TextView) convertView.findViewById(R.id.disambig_text);
            convertView.setTag(holder);
        } else {
            // view already defined, retrieve view holder
            holder = (ViewHolder) convertView.getTag();
        }

        holder.text.setText(Html.fromHtml(items[position]));
        holder.text.setMovementMethod(movementMethod);
        stripUnderlines(holder.text);
        final WikipediaApp app = (WikipediaApp) activity.getApplicationContext();
        app.adjustLinkDrawableToTheme(holder.icon.getDrawable());
        return convertView;
    }

    private void stripUnderlines(TextView textView) {
        Spannable s = (Spannable)textView.getText();
        URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
        for (URLSpan span: spans) {
            int start = s.getSpanStart(span);
            int end = s.getSpanEnd(span);
            s.removeSpan(span);
            span = new URLSpan(span.getURL()) {
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
            };
            s.setSpan(span, start, end, 0);
        }
        textView.setText(s);
    }
}

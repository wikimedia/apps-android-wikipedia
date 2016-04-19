package org.wikipedia.page;

import org.wikipedia.R;
import android.app.Activity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 *
 */
class IssuesListAdapter extends ArrayAdapter<String> {
    private static final String SEPARATOR = "<small><i>(";
    private static final String SEPARATOR_END = ")</i></small>";
    private final Activity activity;
    private final String[] items;

    private ViewHolder holder;

    /**
     * Constructor
     * @param activity The current activity.
     * @param items The objects to represent in the ListView.
     */
    IssuesListAdapter(Activity activity, String[] items) {
        super(activity, 0, items);
        this.activity = activity;
        this.items = items;
    }

    class ViewHolder {
        private ImageView icon;
        private TextView text;
        private TextView subText;
    }

    @Override
    public boolean isEnabled(int position) {
        return false; // don't make it appear clickable
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = activity.getLayoutInflater();
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_issue, null);
            holder = new ViewHolder();
            holder.icon = (ImageView) convertView.findViewById(R.id.issue_icon);
            holder.text = (TextView) convertView.findViewById(R.id.issue_text);
            holder.subText = (TextView) convertView.findViewById(R.id.issue_subtext);
            convertView.setTag(holder);
        } else {
            // view already defined, retrieve view holder
            holder = (ViewHolder) convertView.getTag();
        }

        updateText(position);
        return convertView;
    }

    private void updateText(int position) {
        String fullText = items[position].replaceAll(" href\\s*=", " x="); // disable links
        int end = fullText.lastIndexOf(SEPARATOR_END);
        int start = fullText.lastIndexOf(SEPARATOR, end);
        if (start != -1 && end != -1) {
            String text1 = fullText.substring(0, start);
            String text2 = fullText.substring(start + SEPARATOR.length(), end);
            holder.text.setText(Html.fromHtml(text1));
            holder.subText.setText(Html.fromHtml(text2));
            holder.subText.setVisibility(View.VISIBLE);
        } else {
            holder.text.setText(Html.fromHtml(fullText));
            holder.subText.setVisibility(View.GONE);
        }
    }
}

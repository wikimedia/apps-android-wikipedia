package org.wikipedia.page;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.util.StringUtil;

/**
 *
 */
class IssuesListAdapter extends ArrayAdapter<String> {
    private static final String SEPARATOR = "<small><i>(";
    private static final String SEPARATOR_END = ")</i></small>";
    private final String[] items;

    private ViewHolder holder;

    IssuesListAdapter(@NonNull Context context, @NonNull String[] items) {
        super(context, 0, items);
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

    @Override @NonNull public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_issue, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.issue_icon);
            holder.text = convertView.findViewById(R.id.issue_text);
            holder.subText = convertView.findViewById(R.id.issue_subtext);
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
            holder.text.setText(StringUtil.fromHtml(text1));
            holder.subText.setText(StringUtil.fromHtml(text2));
            holder.subText.setVisibility(View.VISIBLE);
        } else {
            holder.text.setText(StringUtil.fromHtml(fullText));
            holder.subText.setVisibility(View.GONE);
        }
    }
}

package org.wikipedia.page;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.bridge.CommunicationBridge;

/**
 * Handles the #issues links coming from a {@link org.wikipedia.page.PageViewFragment}.
 * Shows a dialog with a list of issues when the issues link is clicked.
 */
public class IssuesHandler implements CommunicationBridge.JSEventListener {
    private static final String SEPARATOR = "<small><i>(";
    private static final String SEPARATOR_END = ")</i></small>";

    private final Activity activity;

    public IssuesHandler(final Activity activity, CommunicationBridge bridge) {
        this.activity = activity;
        bridge.addListener("issuesClicked", this);
    }

    // message from JS bridge:
    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        try {
            show(Utils.jsonArrayToStringArray(messagePayload.getJSONArray("issues")));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void show(final String[] items) {
        final WikipediaApp app = (WikipediaApp) activity.getApplicationContext();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        ListAdapter adapter = new ArrayAdapter<String>(activity, 0, items) {
            private ViewHolder holder;

            class ViewHolder {
                private ImageView icon;
                private TextView text;
                private TextView subText;
            }

            @Override
            public boolean isEnabled(int position) {
                return false; // don't make it appear clickable
            }

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
                app.adjustLinkDrawableToTheme(holder.icon.getDrawable());
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
        };

        builder.setAdapter(adapter, null);
        builder.setTitle(R.string.dialog_page_issues);

        builder.create().show();
    }
}

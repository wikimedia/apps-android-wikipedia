package org.wikipedia.beta.page;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.text.Html;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.beta.R;
import org.wikipedia.beta.Utils;
import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.bridge.CommunicationBridge;

/**
 * Handles the #disambig links coming from a {@link org.wikipedia.beta.page.PageViewFragment}.
 * Automatically goes to the disambiguation page for the selected item.
 */
public class DisambigHandler implements CommunicationBridge.JSEventListener {
    private final Activity activity;
    private LinkHandler linkHandler;
    private Dialog dlg;

    public DisambigHandler(Activity activity, LinkHandler linkHandler, CommunicationBridge bridge) {
        this.activity = activity;
        this.linkHandler = linkHandler;
        bridge.addListener("disambigClicked", this);
    }

    // message from JS bridge:
    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        try {
            show(Utils.jsonArrayToStringArray(messagePayload.getJSONArray("hatnotes")));
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
            }

            @Override
            public boolean isEnabled(int position) {
                return false; // don't make it appear clickable
            }

            public View getView(int position, View convertView, ViewGroup parent) {
                LayoutInflater inflater = activity.getLayoutInflater();
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
                holder.text.setMovementMethod(new LinkMovementMethodExt(linkHandler) {
                    @Override
                    public boolean onTouchEvent(final TextView widget, final Spannable buffer, final MotionEvent event) {
                        boolean ret = super.onTouchEvent(widget, buffer, event);
                        if (ret && event.getAction() == MotionEvent.ACTION_UP && dlg != null) {
                            dlg.dismiss();
                        }
                        return ret;
                    }
                });
                stripUnderlines(holder.text);
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
        };

        builder.setAdapter(adapter, null);
        builder.setTitle(R.string.page_similar_titles);
        dlg = builder.create();
        dlg.show();
    }

}

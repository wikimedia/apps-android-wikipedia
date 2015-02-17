package org.wikipedia.page.gallery;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.page.BottomDialog;
import org.wikipedia.page.LinkMovementMethodExt;

public class DetailsDialog extends BottomDialog {
    private GalleryItem item;
    private LinkMovementMethodExt linkMovementMethod;

    public DetailsDialog(Context context, GalleryItem item, LinkMovementMethodExt linkMovementMethod, int height) {
        super(context, R.layout.dialog_gallery_details);
        this.item = item;
        this.linkMovementMethod = linkMovementMethod;

        View parentView = getDialogLayout();
        View closeButton = parentView.findViewById(R.id.dialog_details_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        parentView.setLayoutParams(
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, height));

        ListView infoList = (ListView) parentView.findViewById(R.id.dialog_details_list);
        infoList.setAdapter(new DetailsListAdapter(item.getMetadata().keySet().toArray()));
    }

    class DetailsListAdapter extends ArrayAdapter<Object> {
        private final Object[] keys;
        private ViewHolder holder;

        public DetailsListAdapter(Object[] items) {
            super(DetailsDialog.this.getContext(), 0, items);
            this.keys = items;
        }

        class ViewHolder {
            private TextView text;
            private TextView subText;
        }

        @Override
        public boolean isEnabled(int position) {
            return false; // don't make it appear clickable
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = DetailsDialog.this.getLayoutInflater();
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_gallery_details, null);
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.details_text);
                holder.subText = (TextView) convertView.findViewById(R.id.details_subtext);
                holder.subText.setMovementMethod(linkMovementMethod);
                convertView.setTag(holder);
            } else {
                // view already defined, retrieve view holder
                holder = (ViewHolder) convertView.getTag();
            }

            holder.text.setText((String)keys[position]);
            holder.subText.setText(Utils.trim(Html.fromHtml(item.getMetadata().get(keys[position]))));
            return convertView;
        }
    }
}

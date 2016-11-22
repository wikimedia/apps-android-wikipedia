package org.wikipedia.page;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.pageimages.PageImagesTask;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ViewUtil;
import org.wikipedia.wikidata.GetDescriptionsTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ListAdapter for disambiguation items.
 */
class DisambigListAdapter extends ArrayAdapter<DisambigResult> {
    private static final int MAX_CACHE_SIZE_IMAGES = 24;
    @NonNull private final LruCache<String, String> pageImagesCache = new LruCache<>(MAX_CACHE_SIZE_IMAGES);
    private final DisambigResult[] items;
    private final WikipediaApp app;
    private final WikiSite wiki;

    DisambigListAdapter(@NonNull Context context, @NonNull DisambigResult[] items) {
        super(context, 0, items);
        this.items = items;
        app = (WikipediaApp) getContext().getApplicationContext();
        wiki = app.getWikiSite();
        requestPageImages();
        fetchDescriptions();
    }

    private void requestPageImages() {
        List<PageTitle> titleList = new ArrayList<>();
        for (DisambigResult r : items) {
            if (pageImagesCache.get(r.getTitle().getPrefixedText()) == null) {
                // not in our cache yet
                titleList.add(r.getTitle());
            }
        }
        if (titleList.isEmpty()) {
            return;
        }

        PageImagesTask imagesTask = new PageImagesTask(
                app.getAPIForSite(wiki),
                wiki,
                titleList,
                (int)(Constants.PREFERRED_THUMB_SIZE * DimenUtil.getDensityScalar())) {
            @Override
            public void onFinish(Map<PageTitle, String> result) {
                for (Map.Entry<PageTitle, String> entry : result.entrySet()) {
                    if (entry.getValue() == null) {
                        continue;
                    }
                    pageImagesCache.put(entry.getKey().getPrefixedText(), entry.getValue());
                }
                notifyDataSetInvalidated();
            }

            @Override
            public void onCatch(Throwable caught) {
                // Don't actually do anything.
                // Thumbnails are expendable
            }
        };
        imagesTask.execute();
    }

    /**
     * Start getting Wikidata descriptions (directly from the current Wikipedia wiki).
     */
    private void fetchDescriptions() {
        List<PageTitle> titleList = new ArrayList<>();
        for (DisambigResult r : items) {
            titleList.add(r.getTitle());
        }
        if (titleList.isEmpty()) {
            return;
        }

        new GetDescriptionsTask(app.getSiteApi(), wiki, titleList) {
            @Override
            public void onFinish(Map<PageTitle, Void> result) {
                notifyDataSetChanged();
            }
            @Override
            public void onCatch(Throwable caught) {
                // descriptions are expendable
            }
        }.execute();
    }

    class ViewHolder {
        private SimpleDraweeView icon;
        private TextView title;
        private TextView description;
    }

    @Override @NonNull public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_page_list_entry, parent, false);
            holder = new ViewHolder();
            holder.icon = (SimpleDraweeView) convertView.findViewById(R.id.page_list_item_image);
            holder.title = (TextView) convertView.findViewById(R.id.page_list_item_title);
            holder.description = (GoneIfEmptyTextView) convertView.findViewById(R.id.page_list_item_description);
            convertView.setTag(holder);
        } else {
            // view already defined, retrieve view holder
            holder = (ViewHolder) convertView.getTag();
        }

        final DisambigResult item = items[position];
        holder.title.setText(item.getTitle().getDisplayText());
        holder.description.setText(StringUtils.capitalize(item.getTitle().getDescription()));

        ViewUtil.loadImageUrlInto(holder.icon, pageImagesCache.get(item.getTitle().getPrefixedText()));
        return convertView;
    }
}

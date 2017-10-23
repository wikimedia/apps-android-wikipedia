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
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.pageimages.PageImagesClient;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ViewUtil;
import org.wikipedia.wikidata.DescriptionClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;

/**
 * ListAdapter for disambiguation items.
 */
class DisambigListAdapter extends ArrayAdapter<DisambigResult> {
    private static final int MAX_CACHE_SIZE_IMAGES = 24;
    @NonNull private final LruCache<String, String> pageImagesCache = new LruCache<>(MAX_CACHE_SIZE_IMAGES);
    private final DisambigResult[] items;
    private final WikiSite wiki = WikipediaApp.getInstance().getWikiSite();

    DisambigListAdapter(@NonNull Context context, @NonNull DisambigResult[] items) {
        super(context, 0, items);
        this.items = items;
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

        new PageImagesClient().request(wiki, titleList,
                new PageImagesClient.Callback() {
                    @Override public void success(@NonNull Call<MwQueryResponse> call,
                                                  @NonNull Map<PageTitle, PageImage> results) {
                        for (Map.Entry<PageTitle, PageImage> entry : results.entrySet()) {
                            if (entry.getValue() == null || entry.getValue().getImageName() == null) {
                                continue;
                            }
                            pageImagesCache.put(entry.getKey().getPrefixedText(), entry.getValue().getImageName());
                        }
                        notifyDataSetInvalidated();
                    }
                    @Override public void failure(@NonNull Call<MwQueryResponse> call,
                                                  @NonNull Throwable caught) {
                        // Don't actually do anything.
                        // Thumbnails are expendable
                    }
                });
    }

    /**
     * Start getting Wikidata descriptions (directly from the current Wikipedia wiki).
     */
    private void fetchDescriptions() {
        final List<PageTitle> titleList = new ArrayList<>();
        for (DisambigResult r : items) {
            titleList.add(r.getTitle());
        }
        if (titleList.isEmpty()) {
            return;
        }

        new DescriptionClient().request(wiki, titleList, new DescriptionClient.Callback() {
            @Override public void success(@NonNull Call<MwQueryResponse> call,
                                          @NonNull List<MwQueryPage> results) {
                for (MwQueryPage page : results) {
                    PageTitle pageTitle = new PageTitle(null, page.title(), wiki);
                    for (PageTitle title : titleList) {
                        if (title.getPrefixedText().equals(pageTitle.getPrefixedText())
                                || title.getDisplayText().equals(pageTitle.getDisplayText())) {
                            title.setDescription(page.description());
                            break;
                        }
                    }
                }
                notifyDataSetChanged();
            }
            @Override public void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught) {
                // descriptions are expendable
            }
        });
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
            holder.icon = convertView.findViewById(R.id.page_list_item_image);
            holder.title = convertView.findViewById(R.id.page_list_item_title);
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

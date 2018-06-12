package org.wikipedia.feed.configure;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.FeedContentType;
import org.wikipedia.util.DimenUtil;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.inflate;

public class LanguageItemAdapter extends RecyclerView.Adapter<LanguageItemHolder>  {
    private Context context;
    private FeedContentType contentType;
    private List<String> langList = new ArrayList<>();

    public LanguageItemAdapter(@NonNull Context context, @NonNull FeedContentType contentType) {
        this.context = context;
        this.contentType = contentType;
        if (contentType.getLangCodesSupported().isEmpty()) {
            // all languages supported
            langList.addAll(WikipediaApp.getInstance().language().getAppLanguageCodes());
        } else {
            // take the intersection of the supported languages and the available app languages
            for (String appLangCode : WikipediaApp.getInstance().language().getAppLanguageCodes()) {
                if (contentType.getLangCodesSupported().contains(appLangCode)) {
                    langList.add(appLangCode);
                }
            }
        }
    }

    public List<String> getLangList() {
        return langList;
    }

    @Override
    public int getItemCount() {
        return langList.size();
    }

    @NonNull
    @Override
    public LanguageItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        View view = inflate(context, R.layout.item_feed_content_type_lang_box, null);
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = DimenUtil.roundedDpToPx(2);
        params.rightMargin = params.leftMargin;
        view.setLayoutParams(params);
        return new LanguageItemHolder(context, view);
    }

    @Override
    public void onBindViewHolder(@NonNull LanguageItemHolder holder, int pos) {
        holder.bindItem(langList.get(pos), !contentType.getLangCodesDisabled().contains(langList.get(pos)));
    }
}

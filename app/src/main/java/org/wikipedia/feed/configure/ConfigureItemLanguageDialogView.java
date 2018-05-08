package org.wikipedia.feed.configure;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.views.DefaultViewHolder;

import java.util.List;

public class ConfigureItemLanguageDialogView extends FrameLayout {
    private List<String> langList;
    private List<String> disabledList;
    private RecyclerView langListView;

    public ConfigureItemLanguageDialogView(Context context) {
        super(context);
        init();
    }

    public ConfigureItemLanguageDialogView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ConfigureItemLanguageDialogView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setContentType(@NonNull List<String> langList, @NonNull List<String> disabledList) {
        this.langList = langList;
        this.disabledList = disabledList;
        langListView.setAdapter(new LanguageItemAdapter());
    }

    private void init() {
        View view = inflate(getContext(), R.layout.item_feed_content_type_lang_select_dialog, this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        langListView = view.findViewById(R.id.feed_content_type_lang_list);
        langListView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private class LanguageItemHolder extends DefaultViewHolder<View> implements OnClickListener {
        private View container;
        private CheckBox checkbox;
        private TextView langNameView;
        private String langCode;

        LanguageItemHolder(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.feed_content_type_lang_container);
            checkbox = itemView.findViewById(R.id.feed_content_type_lang_checkbox);
            langNameView = itemView.findViewById(R.id.feed_content_type_lang_name);
        }

        void bindItem(@NonNull String langCode) {
            this.langCode = langCode;
            container.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
            langNameView.setText(WikipediaApp.getInstance().language().getAppLanguageLocalizedName(langCode));
            container.setOnClickListener(this);
            checkbox.setOnClickListener(this);
            updateState();
        }

        @Override public void onClick(View v) {
            if (disabledList.contains(langCode)) {
                disabledList.remove(langCode);
            } else {
                disabledList.add(langCode);
            }
            updateState();
        }

        private void updateState() {
            boolean enabled = !disabledList.contains(langCode);
            checkbox.setChecked(enabled);
        }
    }

    private final class LanguageItemAdapter extends RecyclerView.Adapter<LanguageItemHolder> {
        @Override
        public int getItemCount() {
            return langList.size();
        }

        @Override
        public LanguageItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View view = inflate(getContext(), R.layout.item_feed_content_type_lang_select_item, null);
            return new LanguageItemHolder(view);
        }

        @Override
        public void onBindViewHolder(LanguageItemHolder holder, int pos) {
            holder.bindItem(langList.get(pos));
        }
    }
}

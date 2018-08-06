package org.wikipedia.onboarding;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class OnboardingPageView extends LinearLayout {
    public interface Callback {
        void onSwitchChange(@NonNull OnboardingPageView view, boolean checked);
        void onLinkClick(@NonNull OnboardingPageView view, @NonNull String url);
        void onListActionButtonClicked(@NonNull OnboardingPageView view);
    }

    public static class DefaultCallback implements Callback {
        @Override
        public void onSwitchChange(@NonNull OnboardingPageView view, boolean checked) { }

        @Override
        public void onLinkClick(@NonNull OnboardingPageView view, @NonNull String url) { }

        @Override
        public void onListActionButtonClicked(@NonNull OnboardingPageView view) { }
    }

    @BindView(R.id.view_onboarding_page_image_centered) ImageView imageViewCentered;
    @BindView(R.id.view_onboarding_page_primary_text) TextView primaryTextView;
    @BindView(R.id.view_onboarding_page_secondary_text) TextView secondaryTextView;
    @BindView(R.id.view_onboarding_page_tertiary_text) TextView tertiaryTextView;
    @BindView(R.id.view_onboarding_page_switch_container) View switchContainer;
    @BindView(R.id.view_onboarding_page_switch) SwitchCompat switchView;
    @BindView(R.id.options_layout) View listViewContainer;
    @BindView(R.id.options_list) RecyclerView optionsList;

    @Nullable private Callback callback;
    @Nullable private String listDataType;
    public OnboardingPageView(Context context) {
        super(context);
        init(null, 0, 0);
    }

    public OnboardingPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0, 0);
    }

    public OnboardingPageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OnboardingPageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void setSwitchChecked(boolean checked) {
        switchView.setChecked(checked);
    }

    @OnCheckedChanged(R.id.view_onboarding_page_switch) void onSwitchChange(boolean checked) {
        if (callback != null) {
            callback.onSwitchChange(this, checked);
        }
    }

    private void init(@Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        setOrientation(getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT ? VERTICAL : HORIZONTAL);
        inflate(getContext(), R.layout.view_onboarding_page, this);
        ButterKnife.bind(this);
        if (attrs != null) {
            TypedArray array = getContext().obtainStyledAttributes(attrs,
                    R.styleable.OnboardingPageView, defStyleAttr, defStyleRes);
            Drawable centeredImage = ContextCompat.getDrawable(getContext(),
                    array.getResourceId(R.styleable.OnboardingPageView_centeredImage, -1));
            String primaryText = array.getString(R.styleable.OnboardingPageView_primaryText);
            String secondaryText = array.getString(R.styleable.OnboardingPageView_secondaryText);
            String tertiaryText = array.getString(R.styleable.OnboardingPageView_tertiaryText);
            String switchText = array.getString(R.styleable.OnboardingPageView_switchText);
            listDataType = array.getString(R.styleable.OnboardingPageView_dataType);
            boolean showListView = array.getBoolean(R.styleable.OnboardingPageView_showListView, false);
            Drawable background = array.getDrawable(R.styleable.OnboardingPageView_background);

            if (background != null) {
                setBackground(background);
            }
            imageViewCentered.setImageDrawable(centeredImage);
            primaryTextView.setText(primaryText);
            secondaryTextView.setText(StringUtil.fromHtml(secondaryText));
            tertiaryTextView.setText(tertiaryText);

            switchContainer.setVisibility(TextUtils.isEmpty(switchText) ? GONE : VISIBLE);
            switchView.setText(switchText);
            setUpListContainer(showListView, listDataType);
            secondaryTextView.setMovementMethod(new LinkMovementMethodExt(
                    (@NonNull String url) -> {
                            if (callback != null) {
                                callback.onLinkClick(OnboardingPageView.this, url);
                            }
                    }));

            array.recycle();
        }
    }

    private void setUpListContainer(boolean showListView, @Nullable String dataType) {
        if (!showListView) {
            return;
        }
        tertiaryTextView.setVisibility(GONE);
        listViewContainer.setVisibility(VISIBLE);
        optionsList.setLayoutManager(new LinearLayoutManager(getContext()));
        optionsList.setAdapter(new ListOptionsAdapter(getListData(dataType)));
    }

    @NonNull
    private List<String> getListData(@Nullable String dataType) {
        List<String> items = new ArrayList<>();
        if (dataType != null && dataType.equals(getContext().getString(R.string.language_data))) {
            for (String code : WikipediaApp.getInstance().language().getAppLanguageCodes()) {
                items.add(StringUtils.capitalize(WikipediaApp.getInstance().language().getAppLanguageLocalizedName(code)));
            }
        }
        return items;
    }

    @OnClick(R.id.add_lang_container)
    void onListActionClicked() {
        if (callback != null) {
            callback.onListActionButtonClicked(this);
        }
    }

    public class ListOptionsAdapter extends RecyclerView.Adapter<ListOptionsAdapter.OptionsViewHolder> {

        private List<String> items;

        ListOptionsAdapter(List<String> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public OptionsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_options_recycler, parent, false);
            return new OptionsViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull OptionsViewHolder holder, int position) {
            holder.optionLabelTextView.setTextDirection(ViewCompat.LAYOUT_DIRECTION_LTR == ViewCompat.getLayoutDirection(primaryTextView) ? TEXT_DIRECTION_LTR : TEXT_DIRECTION_RTL);
            holder.optionLabelTextView.setText(String.format(getContext().getString(R.string.onboarding_option_string), String.valueOf(position + 1), items.get(position)));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class OptionsViewHolder extends RecyclerView.ViewHolder {
            TextView optionLabelTextView;

            OptionsViewHolder(@NonNull View itemView) {
                super(itemView);
                optionLabelTextView = itemView.findViewById(R.id.option_label);
            }
        }
    }

    public void refresh() {
        if (optionsList.getAdapter() != null) {
            optionsList.setAdapter(null);
            optionsList.setAdapter(new ListOptionsAdapter(getListData(listDataType)));
            optionsList.getAdapter().notifyDataSetChanged();
        }
    }

}

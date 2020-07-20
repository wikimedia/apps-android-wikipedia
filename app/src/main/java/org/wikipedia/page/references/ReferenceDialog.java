package org.wikipedia.page.references;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.wikipedia.R;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.page.LinkHandler;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

/**
 * A dialog that displays the currently clicked reference.
 */
public class ReferenceDialog extends ExtendedBottomSheetDialogFragment {
    public interface Callback {
        LinkHandler getLinkHandler();
        List<PageReferences.Reference> getReferencesGroup();
        int getSelectedReferenceIndex();
    }

    @BindView(R.id.reference_pager) ViewPager2 referencesViewPager;
    @BindView(R.id.page_indicator_view) TabLayout pageIndicatorView;
    @BindView(R.id.indicator_divider) View pageIndicatorDivider;
    @BindView(R.id.reference_title_text) TextView titleTextView;
    private Unbinder unbinder;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_references_pager, null);
        unbinder = ButterKnife.bind(this, rootView);
        if (callback() == null || callback().getReferencesGroup() == null) {
            dismiss();
            return rootView;
        }

        titleTextView.setText(requireContext().getString(R.string.reference_title, ""));
        referencesViewPager.setOffscreenPageLimit(2);
        referencesViewPager.setAdapter(new ReferencesAdapter(callback().getReferencesGroup()));
        new TabLayoutMediator(pageIndicatorView, referencesViewPager, (tab, position) -> { }).attach();
        referencesViewPager.setCurrentItem(callback().getSelectedReferenceIndex(), true);

        setConditionalLayoutDirection(rootView, callback().getLinkHandler().getWikiSite().languageCode());
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (callback() != null && callback().getReferencesGroup() != null && callback().getReferencesGroup().size() == 1) {
            pageIndicatorView.setVisibility(View.GONE);
            pageIndicatorDivider.setVisibility(View.GONE);
        } else {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) getView().getParent());
            behavior.setPeekHeight(DimenUtil.getDisplayHeightPx() / 2);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @NonNull
    private String processLinkTextWithAlphaReferences(@NonNull String linkText) {
        boolean isLowercase = linkText.contains("lower");
        if (linkText.contains("alpha ")) {
            String[] strings = linkText.split(" ");
            String alphaReference = StringUtil.getBase26String(Integer.parseInt(strings[strings.length - 1].replace("]", "")));
            alphaReference = isLowercase ? alphaReference.toLowerCase() : alphaReference;
            linkText = alphaReference;
        }
        return linkText.replace("[", "").replace("]", "") + ".";
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new BottomSheetDialog(requireActivity(), getTheme()){
            @Override
            public void onBackPressed() {
                if (referencesViewPager.getCurrentItem() > 0) {
                    referencesViewPager.setCurrentItem(referencesViewPager.getCurrentItem() - 1, true);
                } else {
                    super.onBackPressed();
                }
            }
        };
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        private TextView pagerReferenceText;
        private TextView pagerIdText;

        ViewHolder(View itemView) {
            super(itemView);
            pagerReferenceText = itemView.findViewById(R.id.reference_text);
            pagerReferenceText.setMovementMethod(new LinkMovementMethodExt(callback().getLinkHandler()));
            pagerIdText = itemView.findViewById(R.id.reference_id);
        }

        void bindItem(CharSequence idText, CharSequence contents) {
            pagerIdText.setText(idText);
            pagerReferenceText.setText(contents);
        }
    }

    private class ReferencesAdapter extends RecyclerView.Adapter {
        private List<PageReferences.Reference> references = new ArrayList<>();

        ReferencesAdapter(@NonNull List<PageReferences.Reference> adjacentReferences) {
            references.addAll(adjacentReferences);
        }

        @Override
        public int getItemCount() {
            return references.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View view = inflater.inflate(R.layout.view_reference_pager_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((ViewHolder) holder).bindItem(processLinkTextWithAlphaReferences(references.get(position).getText()),
                    StringUtil.fromHtml(StringUtil.removeCiteMarkup(StringUtil.removeStyleTags(references.get(position).getContent()))));
        }
    }

    @Nullable
    public Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}

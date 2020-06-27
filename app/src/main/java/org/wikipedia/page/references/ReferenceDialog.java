package org.wikipedia.page.references;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.wikipedia.R;
import org.wikipedia.databinding.FragmentReferencesPagerBinding;
import org.wikipedia.page.LinkHandler;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

/**
 * A dialog that displays the currently clicked reference.
 */
public class ReferenceDialog extends BottomSheetDialog {
    private ViewPager2 referencesViewPager;
    private LinkHandler referenceLinkHandler;

    public ReferenceDialog(@NonNull Context context, int selectedIndex, List<PageReferences.Reference> adjacentReferences, LinkHandler referenceLinkHandler) {
        super(context);

        final FragmentReferencesPagerBinding binding = FragmentReferencesPagerBinding.inflate(LayoutInflater.from(context));
        setContentView(binding.getRoot());

        referencesViewPager = binding.referencePager;
        final TabLayout pageIndicatorView = binding.pageIndicatorView;

        this.referenceLinkHandler = referenceLinkHandler;

        if (adjacentReferences.size() == 1) {
            pageIndicatorView.setVisibility(View.GONE);
            ((ViewGroup) pageIndicatorView.getParent()).removeView(pageIndicatorView);
            binding.indicatorDivider.setVisibility(View.GONE);
        } else {
            BottomSheetBehavior behavior = BottomSheetBehavior.from((View) binding.getRoot().getParent());
            behavior.setPeekHeight(DimenUtil.getDisplayHeightPx() / 2);
        }
        binding.referenceTitleText.setText(getContext().getString(R.string.reference_title, ""));

        referencesViewPager.setOffscreenPageLimit(2);
        referencesViewPager.setAdapter(new ReferencesAdapter(adjacentReferences));
        new TabLayoutMediator(pageIndicatorView, referencesViewPager, (tab, position) -> { }).attach();
        referencesViewPager.setCurrentItem(selectedIndex, true);

        setConditionalLayoutDirection(binding.getRoot(), referenceLinkHandler.getWikiSite().languageCode());
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

    @Override
    public void onBackPressed() {
        if (referencesViewPager.getCurrentItem() > 0) {
            referencesViewPager.setCurrentItem(referencesViewPager.getCurrentItem() - 1, true);
        } else {
            super.onBackPressed();
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        private TextView pagerReferenceText;
        private TextView pagerIdText;

        ViewHolder(View itemView) {
            super(itemView);
            pagerReferenceText = itemView.findViewById(R.id.reference_text);
            pagerReferenceText.setMovementMethod(new LinkMovementMethodExt(referenceLinkHandler));
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
}

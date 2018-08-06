package org.wikipedia.page;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.rd.PageIndicatorView;

import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.WrapContentViewPager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A dialog that displays the currently clicked reference.
 */
public class ReferenceDialog extends BottomSheetDialog {
    @BindView(R.id.reference_pager) WrapContentViewPager referencesViewPager;
    @BindView(R.id.pageIndicatorView) PageIndicatorView pageIndicatorView;
    @BindView(R.id.indicator_divider) View pageIndicatorDivider;
    @BindView(R.id.reference_title_text) TextView singleCitationTitleText;
    private LinkHandler referenceLinkHandler;

    ReferenceDialog(@NonNull Context context, int selectedIndex, List<ReferenceHandler.Reference> adjacentReferences, LinkHandler referenceLinkHandler) {
        super(context);
        View rootView = LayoutInflater.from(context).inflate(R.layout.fragment_references_pager, null);
        setContentView(rootView);
        ButterKnife.bind(this);
        this.referenceLinkHandler = referenceLinkHandler;

        if (adjacentReferences.size() == 1) {
            pageIndicatorView.setVisibility(View.GONE);
            ((ViewGroup) pageIndicatorView.getParent()).removeView(pageIndicatorView);
            pageIndicatorDivider.setVisibility(View.GONE);
        } else {
            final int pageIndicatorHeight = 56;
            referencesViewPager.setMaxHeight(DimenUtil.getDisplayHeightPx() / 2 - DimenUtil.roundedDpToPx(pageIndicatorHeight));
            BottomSheetBehavior behavior = BottomSheetBehavior.from((View) rootView.getParent());
            behavior.setPeekHeight(DimenUtil.getDisplayHeightPx() / 2);
        }

        referencesViewPager.setOffscreenPageLimit(2);
        referencesViewPager.setAdapter(new ReferencesAdapter(adjacentReferences));
        pageIndicatorView.setCount(adjacentReferences.size());
        referencesViewPager.setCurrentItem(selectedIndex, true);
    }

    @NonNull private String processLinkTextWithAlphaReferences(@NonNull String linkText) {
        boolean isLowercase = linkText.contains("lower");
        if (linkText.contains("alpha ")) {
            String[] strings = linkText.split(" ");
            String alphaReference = StringUtil.getBase26String(Integer.valueOf(strings[strings.length - 1].replace("]", "")));
            alphaReference = isLowercase ? alphaReference.toLowerCase() : alphaReference;
            return getContext().getString(R.string.add_square_brackets_to_string, alphaReference);
        }
        return linkText;
    }

    @Override
    public void onBackPressed() {
        if (referencesViewPager.getCurrentItem() > 0) {
            referencesViewPager.setCurrentItem(referencesViewPager.getCurrentItem() - 1, true);
        } else {
            super.onBackPressed();
        }
    }

    private class ReferencesAdapter extends PagerAdapter {
        private List<ReferenceHandler.Reference> references = new ArrayList<>();

        ReferencesAdapter(@NonNull List<ReferenceHandler.Reference> adjacentReferences) {
            references.addAll(adjacentReferences);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            LayoutInflater inflater = LayoutInflater.from(container.getContext());
            View view = inflater.inflate(R.layout.view_reference_pager_item, container, false);
            TextView pagerReferenceText = view.findViewById(R.id.reference_text);
            pagerReferenceText.setText(StringUtil.fromHtml(references.get(position).getLinkHtml()));
            pagerReferenceText.setMovementMethod(new LinkMovementMethodExt(referenceLinkHandler));

            TextView pagerTitleText = view.findViewById(R.id.reference_title_text);
            pagerTitleText.setText(getContext().getString(R.string.reference_title, processLinkTextWithAlphaReferences(references.get(position).getLinkText())));
            container.addView(view);

            return view;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            View view = ((View) object);
            container.removeView(view);
        }

        @Override
        public int getCount() {
            return references.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }
}

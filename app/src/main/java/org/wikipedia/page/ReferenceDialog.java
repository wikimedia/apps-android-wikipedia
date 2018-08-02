package org.wikipedia.page;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.rd.PageIndicatorView;

import org.wikipedia.R;
import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A dialog that displays the currently clicked reference.
 */
public class ReferenceDialog extends BottomSheetDialog {
    @BindView(R.id.reference_pager) ViewPager referencesViewPager;
    @BindView(R.id.pageIndicatorView) PageIndicatorView pageIndicatorView;
    @BindView(R.id.indicator_layout) View referencePagerIndicatorLayout;
    @BindView(R.id.multiple_citations_layout) View multipleCitationsContainer;
    @BindView(R.id.single_citation_layout) View singleCitationContainer;
    @BindView(R.id.reference_text) TextView singleCitationReferenceText;
    @BindView(R.id.reference_title_text) TextView singleCitationTitleText;
    private LinkHandler referenceLinkHandler;

    ReferenceDialog(@NonNull Context context, int selectedIndex, List<ReferenceHandler.Reference> adjacentReferences, LinkHandler referenceLinkHandler) {
        super(context);
        View rootView = LayoutInflater.from(context).inflate(R.layout.fragment_references_pager, null);
        setContentView(rootView);
        ButterKnife.bind(this);
        this.referenceLinkHandler = referenceLinkHandler;
        referencesViewPager.setAdapter(new ReferencesAdapter(adjacentReferences));
        referencesViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                pageIndicatorView.setSelection(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        if (adjacentReferences.size() > 1) {
            multipleCitationsContainer.setVisibility(View.VISIBLE);
            singleCitationContainer.setVisibility(View.GONE);
            pageIndicatorView.setCount(adjacentReferences.size());
            referencesViewPager.setCurrentItem(selectedIndex, true);
        } else {
            multipleCitationsContainer.setVisibility(View.GONE);
            singleCitationContainer.setVisibility(View.VISIBLE);
            singleCitationReferenceText.setText(StringUtil.fromHtml(adjacentReferences.get(0).getLinkHtml()));
            singleCitationTitleText.setText(getContext().getString(R.string.reference_title, adjacentReferences.get(0).getLinkText()));
        }
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
            pagerTitleText.setText(getContext().getString(R.string.reference_title, references.get(position).getLinkText()));
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

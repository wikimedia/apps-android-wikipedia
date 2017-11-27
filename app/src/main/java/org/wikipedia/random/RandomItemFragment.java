package org.wikipedia.random;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.page.PageTitle;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.GoneIfEmptyTextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import retrofit2.Call;

public class RandomItemFragment extends Fragment {
    @BindView(R.id.random_item_container) ViewGroup containerView;
    @BindView(R.id.random_item_progress) View progressBar;
    @BindView(R.id.view_featured_article_card_image) FaceAndColorDetectImageView imageView;
    @BindView(R.id.view_featured_article_card_article_title) TextView articleTitleView;
    @BindView(R.id.view_featured_article_card_article_subtitle) GoneIfEmptyTextView articleSubtitleView;
    @BindView(R.id.view_featured_article_card_extract) TextView extractView;
    private Unbinder unbinder;

    @Nullable private RbPageSummary summary;
    private int pagerPosition = -1;

    @NonNull
    public static RandomItemFragment newInstance() {
        return new RandomItemFragment();
    }

    public void setPagerPosition(int position) {
        pagerPosition = position;
    }

    public int getPagerPosition() {
        return pagerPosition;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_random_item, container, false);
        unbinder = ButterKnife.bind(this, view);
        imageView.setLegacyVisibilityHandlingEnabled(true);
        setContents(null);

        new RandomSummaryClient().request(WikipediaApp.getInstance().getWikiSite(), new RandomSummaryClient.Callback() {
            @Override
            public void onSuccess(@NonNull Call<RbPageSummary> call, @NonNull RbPageSummary pageSummary) {
                if (!isAdded()) {
                    return;
                }
                setContents(pageSummary);
            }

            @Override
            public void onError(@NonNull Call<RbPageSummary> call, @NonNull Throwable t) {
                // TODO: show error.
            }
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @OnClick(R.id.view_featured_article_card_text_container) void onClick(View v) {
        if (getTitle() != null) {
            parent().onSelectPage(getTitle());
        }
    }

    public void setContents(@Nullable RbPageSummary pageSummary) {
        containerView.setVisibility(pageSummary == null ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(pageSummary == null ? View.VISIBLE : View.GONE);
        if (summary == pageSummary) {
            return;
        }
        summary = pageSummary;
        if (summary == null) {
            return;
        }
        articleTitleView.setText(summary.getNormalizedTitle());
        articleSubtitleView.setText(null); //summary.getDescription());
        extractView.setText(summary.getExtract());
        ViewTreeObserver observer = extractView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                extractView.setMaxLines(extractView.getHeight() / extractView.getLineHeight());
                extractView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        imageView.loadImage(TextUtils.isEmpty(summary.getThumbnailUrl()) ? null
                : Uri.parse(summary.getThumbnailUrl()));
    }

    @Nullable public PageTitle getTitle() {
        return summary == null ? null
                : new PageTitle(summary.getTitle(), WikipediaApp.getInstance().getWikiSite());
    }

    private RandomFragment parent() {
        return (RandomFragment) getActivity().getSupportFragmentManager().getFragments().get(0);
    }
}

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
import org.wikipedia.util.log.L;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.WikiErrorView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;

public class RandomItemFragment extends Fragment {
    @BindView(R.id.random_item_container) ViewGroup containerView;
    @BindView(R.id.random_item_progress) View progressBar;
    @BindView(R.id.view_featured_article_card_image) FaceAndColorDetectImageView imageView;
    @BindView(R.id.view_featured_article_card_article_title) TextView articleTitleView;
    @BindView(R.id.view_featured_article_card_article_subtitle) GoneIfEmptyTextView articleSubtitleView;
    @BindView(R.id.view_featured_article_card_extract) TextView extractView;
    @BindView(R.id.random_item_error_view) WikiErrorView errorView;

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

    public boolean isLoadComplete() {
        return summary != null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_random_item, container, false);
        ButterKnife.bind(this, view);
        imageView.setLegacyVisibilityHandlingEnabled(true);
        errorView.setBackClickListener(v -> getActivity().finish());
        errorView.setRetryClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            getRandomPage();
        });
        updateContents();
        if (summary == null) {
            getRandomPage();
        }
        return view;
    }

    private void getRandomPage() {
        new RandomSummaryClient().request(WikipediaApp.getInstance().getWikiSite(), new RandomSummaryClient.Callback() {
            @Override
            public void onSuccess(@NonNull Call<RbPageSummary> call, @NonNull RbPageSummary pageSummary) {
                summary = pageSummary;
                if (!isAdded()) {
                    return;
                }
                updateContents();
                parent().onChildLoaded();
            }

            @Override
            public void onError(@NonNull Call<RbPageSummary> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                setErrorState(t);
            }
        });
    }

    private void setErrorState(@NonNull Throwable t) {
        L.e(t);
        errorView.setError(t);
        errorView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        containerView.setVisibility(View.GONE);
    }

    @OnClick(R.id.view_featured_article_card_text_container) void onClick(View v) {
        if (getTitle() != null) {
            parent().onSelectPage(getTitle());
        }
    }

    public void updateContents() {
        errorView.setVisibility(View.GONE);
        containerView.setVisibility(summary == null ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(summary == null ? View.VISIBLE : View.GONE);
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
                if (!isAdded() || extractView == null) {
                    return;
                }
                int maxLines = extractView.getHeight() / extractView.getLineHeight() - 1;
                final int minLines = 3;
                extractView.setMaxLines(Math.max(maxLines, minLines));
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

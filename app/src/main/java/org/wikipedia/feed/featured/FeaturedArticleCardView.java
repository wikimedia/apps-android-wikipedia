package org.wikipedia.feed.featured;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.feed.view.BigPictureCardView;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.FeaturedCardFooterView;

public class FeaturedArticleCardView extends BigPictureCardView {

    public FeaturedArticleCardView(Context context) {
        super(context);
    }

    public void set(@NonNull FeaturedArticleCard card) {
        String articleTitle = card.articleTitle();
        String articleSubtitle = card.articleSubtitle();
        String extract = card.extract();
        Uri imageUri = card.image();

        articleTitle(articleTitle);
        articleSubtitle(articleSubtitle);
        extract(extract);
        image(imageUri);

        header(card);
        footer();
    }

    private void header(@NonNull FeaturedArticleCard card) {
        CardHeaderView header = new CardHeaderView(getContext())
                .setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setImage(R.drawable.ic_star_black_24dp)
                .setImageCircleColor(R.color.feed_featured_icon_background);
        header(header);
    }

    private void footer() {
        footer(new FeaturedCardFooterView(getContext()));
    }
}

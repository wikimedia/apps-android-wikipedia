package org.wikipedia.feed.announcement;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.GeoUtil;

public class FundraisingCardView extends AnnouncementCardView {
    public FundraisingCardView(@NonNull Context context) {
        super(context);
        setNegativeActionVisible(true);
        setHeaderImageVisible(true);
        setHeaderImage(R.drawable.ic_fundraising_header_2016);

        String footerStr = String.format(context.getString(R.string.view_fundraising_card_footer),
                GeoUtil.getGeoIPCountry(),
                WikipediaApp.getInstance().getWikiSite().languageCode());

        setFooterText(footerStr);
    }
}

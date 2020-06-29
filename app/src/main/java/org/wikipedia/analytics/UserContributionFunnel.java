package org.wikipedia.analytics;

import org.wikipedia.WikipediaApp;

public final class UserContributionFunnel extends Funnel {
    private static UserContributionFunnel INSTANCE;

    private static final String SCHEMA_NAME = "MobileWikiAppUserContribution";
    private static final int REV_ID = 20217330;

    private UserContributionFunnel() {
        super(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL);
    }

    public static UserContributionFunnel get() {
        if (INSTANCE == null) {
            INSTANCE = new UserContributionFunnel();
        }
        return INSTANCE;
    }

    public static void reset() {
        INSTANCE = null;
    }

    public void logOpen() {
        log("action", "open_hist");
    }

    public void logFilterDescriptions() {
        log("action", "filt_desc");
    }

    public void logFilterCaptions() {
        log("action", "filt_caption");
    }

    public void logFilterTags() {
        log("action", "filt_tag");
    }

    public void logFilterAll() {
        log("action", "filt_all");
    }

    public void logViewDescription() {
        log("action", "desc_view");
    }

    public void logViewCaption() {
        log("action", "caption_view");
    }

    public void logViewTag() {
        log("action", "tag_view");
    }

    public void logViewMisc() {
        log("action", "misc_view");
    }

    public void logNavigateDescription() {
        log("action", "desc_view2");
    }

    public void logNavigateCaption() {
        log("action", "caption_view2");
    }

    public void logNavigateTag() {
        log("action", "tag_view2");
    }

    public void logNavigateMisc() {
        log("action", "misc_view2");
    }

    public void logPaused() {
        log("action", "paused");
    }

    public void logDisabled() {
        log("action", "disabled");
    }

    public void logIpBlock() {
        log("action", "ip_block");
    }
}

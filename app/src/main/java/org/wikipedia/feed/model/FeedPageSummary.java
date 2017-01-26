package org.wikipedia.feed.model;


import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.json.annotations.Required;

/**
 * An RbPageSummary plus a "normalizedtitle" field, injected by RESTBase for all page summary
 * objects in the aggregated feed endpoint (/feed/featured/yyyy/mm/dd).  Used in the "tfa' (Today's
 * Featured Article), "mostread", and "news" sections of the aggregated feed content response.
 *
 * N.B.: In contrast to the RbPageSummary base class, "title" here is the un-normalized, raw title,
 * and the normalized title is sent is "normalizedtitle".  In an RbPageSummary, the "title" property
 * contains the normalized title.
 */
public class FeedPageSummary extends RbPageSummary {
    @Required @NonNull @SuppressWarnings("unused,NullableProblems") @SerializedName("normalizedtitle")
    private String normalizedTitle;

    @NonNull public String getNormalizedTitle() {
        return normalizedTitle;
    }
}

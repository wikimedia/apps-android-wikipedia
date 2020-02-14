package org.wikipedia.push;

import androidx.annotation.Nullable;

public class PushServiceSubscriptionResponse {
    private long created;
    private long updated;
    @Nullable private String id;
    @Nullable private String proto;
    @Nullable private String token;
    @Nullable private String lang;

    @Nullable public String id() {
        return id;
    }
}

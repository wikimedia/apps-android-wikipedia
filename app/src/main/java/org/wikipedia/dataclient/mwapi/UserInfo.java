package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import org.apache.commons.lang3.StringUtils;
import org.threeten.bp.Instant;
import org.wikipedia.util.DateUtil;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class UserInfo {
    private String name;
    private int id;
    @Nullable private List<String> groups;
    private int blockid;
    @Nullable private String blockreason;
    @Nullable private String blockedby;
    @Nullable private String blockedtimestamp;
    @Nullable private String blockexpiry;

    public int id() {
        return id;
    }

    @NonNull public Set<String> getGroups() {
        return groups != null ? new ArraySet<>(groups) : Collections.emptySet();
    }

    public boolean isBlocked() {
        return StringUtils.isNotEmpty(blockexpiry)
                && DateUtil.iso8601InstantParse(blockexpiry).isAfter(Instant.now());
    }
}

package org.wikipedia.dataclient.mwapi;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class UserInfo extends MwServiceError.BlockInfo {
    @Nullable private String name;
    private int id;
    @Nullable private List<String> groups;
    private int editcount;
    @Nullable private String latestcontrib;

    public String getName() {
        return StringUtils.defaultString(name);
    }

    public int id() {
        return id;
    }

    @NonNull public Set<String> getGroups() {
        return groups != null ? new ArraySet<>(groups) : Collections.emptySet();
    }

    public int getEditCount() {
        return editcount;
    }

    @NonNull public LocalDate getLatestContrib() {
        return TextUtils.isEmpty(latestcontrib) ? LocalDate.MIN
                : Instant.parse(latestcontrib).atZone(ZoneOffset.UTC).toLocalDate();
    }
}

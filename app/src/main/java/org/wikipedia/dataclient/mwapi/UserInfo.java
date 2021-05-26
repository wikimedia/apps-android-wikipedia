package org.wikipedia.dataclient.mwapi;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import org.wikipedia.util.DateUtil;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class UserInfo extends MwServiceError.BlockInfo {
    private String name;
    private int id;
    @Nullable private List<String> groups;
    private int editcount;
    @Nullable private String latestcontrib;

    public int id() {
        return id;
    }

    @NonNull public Set<String> getGroups() {
        return groups != null ? new ArraySet<>(groups) : Collections.emptySet();
    }

    public int getEditCount() {
        return editcount;
    }

    @NonNull public Date getLatestContrib() {
        Date date = new Date(0);
        if (!TextUtils.isEmpty(latestcontrib)) {
            date = DateUtil.iso8601DateParse(latestcontrib);
        }
        return date;
    }
}

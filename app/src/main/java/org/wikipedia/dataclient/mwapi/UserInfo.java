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
public class UserInfo {
    private String name;
    private int id;
    @Nullable private List<String> groups;
    private int blockid;
    private int editcount;
    @Nullable private String latestcontrib;
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

    public boolean isBlocked() {
        if (TextUtils.isEmpty(blockexpiry)) {
            return false;
        }
        Date now = new Date();
        Date expiry = DateUtil.iso8601DateParse(blockexpiry);
        return expiry.after(now);
    }
}

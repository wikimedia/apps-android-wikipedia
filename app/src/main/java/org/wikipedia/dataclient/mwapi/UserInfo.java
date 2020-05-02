package org.wikipedia.dataclient.mwapi;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import org.apache.commons.collections4.ListUtils;
import org.wikipedia.util.DateUtil;

import java.text.ParseException;
import java.util.Date;
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
        return new ArraySet<>(ListUtils.emptyIfNull(groups));
    }

    public boolean isBlocked() {
        if (TextUtils.isEmpty(blockexpiry)) {
            return false;
        }
        try {
            Date now = new Date();
            Date expiry = DateUtil.iso8601DateParse(blockexpiry);
            if (expiry.after(now)) {
                return true;
            }
        } catch (ParseException e) {
            // ignore
        }
        return false;
    }
}

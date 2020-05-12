package org.wikipedia.dataclient.mwapi;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import org.wikipedia.util.DateUtil;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
    @Nullable private String registrationdate;

    public int id() {
        return id;
    }

    @NonNull public Set<String> getGroups() {
        return groups != null ? new ArraySet<>(groups) : Collections.emptySet();
    }

    @SuppressWarnings("checkstyle:magicnumber")
    public boolean passesSemiProtectionOnCommons() {
        // Reference: https://commons.wikimedia.org/wiki/Commons:Protection_policy
        if (TextUtils.isEmpty(registrationdate)) {
            return false;
        }

        try {
            Date registrationDate = DateUtil.iso8601DateParse(registrationdate);
            long diffDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - registrationDate.getTime());
            if (diffDays > 4) {
                return true;
            }
        } catch (ParseException e) {
            // ignore
        }

        return false;
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

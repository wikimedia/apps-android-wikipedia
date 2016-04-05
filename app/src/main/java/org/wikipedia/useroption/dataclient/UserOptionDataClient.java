package org.wikipedia.useroption.dataclient;

import android.support.annotation.NonNull;

import org.wikipedia.useroption.UserOption;

public interface UserOptionDataClient {
    @NonNull UserInfo get();
    void post(@NonNull UserOption option);
    void delete(@NonNull String key);
}
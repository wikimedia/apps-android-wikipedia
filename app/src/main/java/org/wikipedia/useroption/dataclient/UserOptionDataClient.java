package org.wikipedia.useroption.dataclient;

import android.support.annotation.NonNull;

import org.wikipedia.useroption.UserOption;

import java.io.IOException;

public interface UserOptionDataClient {
    void get(@NonNull DefaultUserOptionDataClient.UserInfoCallback callback);
    void post(@NonNull UserOption option) throws IOException;
    void delete(@NonNull String key) throws IOException;
}

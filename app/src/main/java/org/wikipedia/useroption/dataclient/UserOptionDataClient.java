package org.wikipedia.useroption.dataclient;

import android.support.annotation.NonNull;

import org.wikipedia.useroption.UserOption;

import java.io.IOException;

public interface UserOptionDataClient {
    @NonNull UserInfo get() throws IOException;
    void post(@NonNull UserOption option) throws IOException;
    void delete(@NonNull String key) throws IOException;
}
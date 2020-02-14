package org.wikipedia.util;

public interface Callback<A, B> {
    void onSuccess(A arg);
    void onFailure(B arg);
}

package org.wikipedia.descriptions.centralauth;

import org.wikipedia.server.mwapi.MwServiceError;

public class CentralAuthTokenRetrievalFailedException extends Exception {
    private final MwServiceError error;

    public CentralAuthTokenRetrievalFailedException(MwServiceError error) {
        super(error.getTitle());
        this.error = error;
    }

    public MwServiceError getError() {
        return error;
    }
}

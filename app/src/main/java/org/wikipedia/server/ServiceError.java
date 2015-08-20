package org.wikipedia.server;

/**
 * The API reported an error in the payload.
 */
public interface ServiceError {
    String getCode();

    String getInfo();

    String getDocRef();
}

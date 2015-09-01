package org.wikipedia.server;

/**
 * The API reported an error in the payload.
 */
public interface ServiceError {
    String getTitle();

    String getDetails();
}

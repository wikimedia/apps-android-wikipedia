package org.wikipedia.readinglist.api;

/**
 * A list of pages that are interesting to read later.
 */
public interface ReadingList {

    /** @return the ID of this collection */
    int getId();

    /** @return the user visible label of this collection */
    String getLabel();

    /** @return the timestamp of when this collection was last updated */
    String getLastUpdated();

    /** @return the number of pages contained in this collection */
    int getCount();

    /** @return a link to a thumbnail URL for this collection */
    String getImageUrl();
}

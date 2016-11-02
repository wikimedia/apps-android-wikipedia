package org.wikipedia.descriptions;

import org.wikipedia.dataclient.mwapi.MwPostResponse;

/**
 * JSON response of a Wikidata description edit request.
 */
class DescriptionEdit extends MwPostResponse {
    private int success;

    boolean editWasSuccessful() {
        return success > 0;
    }

    /*
        There are many more fields but we only use the success flag and any potential error cases
        handled by the superclass. Examples of what's available from the server response are in
        description_edit.json and description_edit_unknown_site.json.
     */
}

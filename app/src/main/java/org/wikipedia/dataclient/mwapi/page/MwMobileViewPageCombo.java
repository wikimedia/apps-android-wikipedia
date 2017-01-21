package org.wikipedia.dataclient.mwapi.page;

import org.wikipedia.dataclient.page.PageCombo;

/**
 * Combines MwMobileViewPageLead and MwMobileViewPageRemaining Gson POJOs for mobileview API.
 * In mobileview API the implementation is basically the same as MwMobileViewPageLead.
 * The class name "Page" was already used, and is very entrenched in this code base.
 */
public class MwMobileViewPageCombo extends MwMobileViewPageLead implements PageCombo {
}

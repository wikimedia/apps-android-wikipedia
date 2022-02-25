package org.wikipedia.page.edithistory

import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.restbase.EditCount
import org.wikipedia.dataclient.restbase.Metrics

class EditStats(val revision: MwQueryPage.Revision, val editCount: EditCount, val metrics: List<Metrics.Results>)

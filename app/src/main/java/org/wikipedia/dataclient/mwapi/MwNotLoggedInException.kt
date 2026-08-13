package org.wikipedia.dataclient.mwapi

class MwNotLoggedInException(error: MwServiceError) : MwException(error)

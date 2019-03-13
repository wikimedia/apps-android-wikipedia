package org.wikipedia.editactionfeed

class MyContributions {
    // TODO: need to update the entire class once the endpoint is completed.

    var list: List<EditCount>? = null

    class EditCount {
        var languageCode: String? = null
        var editCount: Int? = null
    }
}

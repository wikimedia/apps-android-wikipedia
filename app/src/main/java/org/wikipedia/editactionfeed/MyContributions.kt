package org.wikipedia.editactionfeed

class MyContributions {
    // TODO: need to update the entire class once the endpoint is completed.

    var level: Int? = null
    var editCount: Int? = null
    var list: List<ContributionType>? = null


    class ContributionType {
        var typeCode: Int? = null
        var typeTitle: String? = null
        var list: List<EditCount>? = null
    }

    class EditCount {
        var languageCode: String? = null
        var editCount: Int? = null
    }
}

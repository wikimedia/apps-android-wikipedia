package org.wikimedia.testkitchen.curation

import org.wikimedia.testkitchen.JsonUtil
import org.wikimedia.testkitchen.config.CurationFilter

object CurationFilterFixtures {
    fun curationFilter(): CurationFilter {
        val curationFilterJson =
            "{\"page_id\":{\"less_than\":500,\"not_equals\":42},\"page_namespace_text\":" +
                    "{\"equals\":\"Talk\"},\"user_is_logged_in\":{\"equals\":true},\"user_edit_count_bucket\":" +
                    "{\"in\":[\"100-999 edits\",\"1000+ edits\"]},\"user_groups\":{\"contains_all\":" +
                    "[\"user\",\"autoconfirmed\"],\"does_not_contain\":\"sysop\"}}"
        return JsonUtil.decodeFromString<CurationFilter>(curationFilterJson)!!
    }
}

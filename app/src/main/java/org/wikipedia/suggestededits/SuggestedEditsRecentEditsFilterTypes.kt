package org.wikipedia.suggestededits

import androidx.annotation.StringRes
import org.wikipedia.R
import org.wikipedia.model.EnumCode
import org.wikipedia.model.EnumCodeMap

@Suppress("unused")
enum class SuggestedEditsRecentEditsFilterTypes constructor(val id: String,
                                                            val value: String,
                                                            @StringRes val title: Int,
                                                            @StringRes val description: Int? = null
) : EnumCode {
    ALL_EDITS("allEdits", "",
        R.string.patroller_tasks_filters_all_text),
    MINOR_EDITS("minorEdits", "minor",
        R.string.patroller_tasks_filters_significance_minor),
    NON_MINOR_EDITS("nonMinorEdits", "!minor",
        R.string.patroller_tasks_filters_significance_edits),
    ALL_EDITORS("allEditors", "",
        R.string.patroller_tasks_filters_all_text),
    BOT("bot", "bot",
        R.string.patroller_tasks_filters_automated_contributions_bot),
    HUMAN("human", "!bot",
        R.string.patroller_tasks_filters_automated_contributions_human),
    LATEST_REVISION("latestRevision", "",
        R.string.patroller_tasks_filters_latest_revisions_latest_revision),
    NOT_LATEST_REVISION("notLatestRevision", "1",
        R.string.patroller_tasks_filters_latest_revisions_not_latest_revision),
    UNREGISTERED("unregistered", "anon",
        R.string.patroller_tasks_filters_user_status_unregistered, R.string.patroller_tasks_filters_user_status_unregistered_desc),
    REGISTERED("registered", "!anon",
        R.string.patroller_tasks_filters_user_status_registered, R.string.patroller_tasks_filters_user_status_registered_desc),
    // TODO: SPIKE ====
    NEWCOMERS("newcomers", "0,10|0,4",
        R.string.patroller_tasks_filters_user_status_newcomers, R.string.patroller_tasks_filters_user_status_newcomers_desc),
    LEARNERS("learners", "10,500|4,30",
        R.string.patroller_tasks_filters_user_status_learners, R.string.patroller_tasks_filters_user_status_learners_desc),
    EXPERIENCED_USERS("experiencedUsers", "500,-1|30,-1",
        R.string.patroller_tasks_filters_user_status_experienced, R.string.patroller_tasks_filters_user_status_experienced_desc),
    // TODO: SPIKE ====
    // ORES score reference: https://github.com/wikimedia/mediawiki-extensions-ORES/blob/master/extension.json#L201-L232C28
    // Only check the damaging: { true } score
    DAMAGING_GOOD("damagingGood", "0|0.149",
        R.string.patroller_tasks_filters_contribution_quality_good, R.string.patroller_tasks_filters_contribution_quality_good_desc),
    DAMAGING_MAY_PROBLEMS("damagingMayProblems", "0.149|0.629",
        R.string.patroller_tasks_filters_contribution_quality_may_problems, R.string.patroller_tasks_filters_contribution_quality_may_problems_desc),
    DAMAGING_LIKELY_PROBLEMS("damagingLikelyProblems", "0.629|0.944",
        R.string.patroller_tasks_filters_contribution_quality_likely_problems, R.string.patroller_tasks_filters_contribution_quality_likely_problems_desc),
    DAMAGING_VERY_LIKELY_PROBLEMS("damagingBad", "0.944|1",
        R.string.patroller_tasks_filters_contribution_quality_bad, R.string.patroller_tasks_filters_contribution_quality_bad_desc),
    // Only check the goodfaith: { true } score
    GOODFAITH_GOOD("goodfaithGood", "0.75|1",
        R.string.patroller_tasks_filters_user_intent_good, R.string.patroller_tasks_filters_user_intent_good_desc),
    GOODFAITH_MAY_PROBLEMS("goodfaithMayProblems", "0.647|0.75",
        R.string.patroller_tasks_filters_user_intent_may_problems, R.string.patroller_tasks_filters_user_intent_may_problems_desc),
    GOODFAITH_LIKELY_PROBLEMS("goodfaithLikelyProblems", "0.25|0.647",
        R.string.patroller_tasks_filters_user_intent_likely_problems, R.string.patroller_tasks_filters_user_intent_likely_problems_desc),
    GOODFAITH_VERY_LIKELY_PROBLEMS("goodfaithBad", "0|0.25",
        R.string.patroller_tasks_filters_user_intent_bad, R.string.patroller_tasks_filters_user_intent_bad_desc);

    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal
    }

    companion object {
        val MINOR_EDITS_GROUP = listOf(ALL_EDITS, MINOR_EDITS, NON_MINOR_EDITS)
        val BOT_EDITS_GROUP = listOf(ALL_EDITORS, BOT, HUMAN)
        val LATEST_REVISIONS_GROUP = listOf(LATEST_REVISION, NOT_LATEST_REVISION)
        val USER_REGISTRATION_GROUP = listOf(UNREGISTERED, REGISTERED)
        val USER_EXPERIENCE_GROUP = listOf(NEWCOMERS, LEARNERS, EXPERIENCED_USERS)
        val DAMAGING_GROUP = listOf(DAMAGING_GOOD, DAMAGING_MAY_PROBLEMS, DAMAGING_LIKELY_PROBLEMS, DAMAGING_VERY_LIKELY_PROBLEMS)
        val GOODFAITH_GROUP = listOf(GOODFAITH_GOOD, GOODFAITH_MAY_PROBLEMS, GOODFAITH_LIKELY_PROBLEMS, GOODFAITH_VERY_LIKELY_PROBLEMS)

        // Multiple choice
        val DEFAULT_FILTER_USER_STATUS = setOf(UNREGISTERED, NEWCOMERS)

        // Single choice
        val DEFAULT_FILTER_OTHERS = setOf(ALL_EDITS, LATEST_REVISION, ALL_EDITORS)

        val DEFAULT_FILTER_TYPE_SET = DEFAULT_FILTER_OTHERS + DEFAULT_FILTER_USER_STATUS

        private val MAP = EnumCodeMap(SuggestedEditsRecentEditsFilterTypes::class.java)

        private fun findOrNull(id: String): SuggestedEditsRecentEditsFilterTypes? {
            return MAP.valueIterator().asSequence().firstOrNull { id == it.id || id.startsWith(it.id) }
        }

        fun find(id: String): SuggestedEditsRecentEditsFilterTypes {
            return findOrNull(id) ?: MAP[0]
        }

        fun findGroup(id: String): List<SuggestedEditsRecentEditsFilterTypes> {
            val groups = listOf(MINOR_EDITS_GROUP, BOT_EDITS_GROUP, LATEST_REVISIONS_GROUP)
            return groups.find { it.contains(find(id)) }.orEmpty()
        }
    }
}

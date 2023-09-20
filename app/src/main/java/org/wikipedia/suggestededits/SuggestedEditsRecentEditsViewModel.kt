package org.wikipedia.suggestededits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.dataclient.mwapi.UserInfo
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import retrofit2.HttpException
import java.io.IOException
import java.time.Duration
import java.util.Calendar
import java.util.Date

class SuggestedEditsRecentEditsViewModel : ViewModel() {

    var langCode = Prefs.recentEditsWikiCode

    val wikiSite get(): WikiSite {
        return when (langCode) {
            Constants.WIKI_CODE_COMMONS -> WikiSite(Service.COMMONS_URL)
            Constants.WIKI_CODE_WIKIDATA -> WikiSite(Service.WIKIDATA_URL)
            else -> WikiSite.forLanguageCode(langCode)
        }
    }
    var currentQuery = ""
    var actionModeActive = false
    var recentEditsSource: RecentEditsPagingSource? = null

    private val cachedUserInfo = mutableListOf<UserInfo>()

    val recentEditsFlow = Pager(PagingConfig(pageSize = 50, initialLoadSize = 50), pagingSourceFactory = {
        recentEditsSource = RecentEditsPagingSource()
        recentEditsSource!!
    }).flow.map { pagingData ->
        pagingData.filter {
            if (currentQuery.isNotEmpty()) {
                it.parsedComment.contains(currentQuery, true) ||
                        it.title.contains(currentQuery, true) ||
                        it.user.contains(currentQuery, true) ||
                        it.joinedTags.contains(currentQuery, true) ||
                        it.parsedDateTime.toString().contains(currentQuery, true)
            } else true
        }.map {
            RecentEditsItem(it)
        }.insertSeparators { before, after ->
            val dateBefore = before?.item?.parsedDateTime?.toLocalDate()
            val dateAfter = after?.item?.parsedDateTime?.toLocalDate()
            if (dateAfter != null && dateAfter != dateBefore) {
                RecentEditsSeparator(DateUtil.getShortDateString(dateAfter))
            } else {
                null
            }
        }
    }.cachedIn(viewModelScope)

    fun clearCache() {
        cachedUserInfo.clear()
    }

    fun filtersCount(): Int {
        val findSelectedUserStatus = Prefs.recentEditsIncludedTypeCodes
            .filter { code ->
                SuggestedEditsRecentEditsFilterTypes.USER_REGISTRATION_GROUP.map { it.id }.contains(code) ||
                SuggestedEditsRecentEditsFilterTypes.USER_EXPERIENCE_GROUP.map { it.id }.contains(code)
            }

        // It should include: "not" default values + "non-selected" default values
        val defaultUserStatusSet = SuggestedEditsRecentEditsFilterTypes.DEFAULT_FILTER_USER_STATUS.map { it.id }.toSet()
        val nonDefaultUserStatus = findSelectedUserStatus.subtract(defaultUserStatusSet)
            .union(defaultUserStatusSet.subtract(findSelectedUserStatus.toSet()))

        // Ores related: default is empty
        val findSelectedOres = Prefs.recentEditsIncludedTypeCodes.subtract(findSelectedUserStatus.toSet())
            .filter { code ->
                SuggestedEditsRecentEditsFilterTypes.GOODFAITH_GROUP.map { it.id }.contains(code) ||
                        SuggestedEditsRecentEditsFilterTypes.DAMAGING_GROUP.map { it.id }.contains(code)
            }

        // Find the remaining selected filters
        val findSelectedOthers = Prefs.recentEditsIncludedTypeCodes.subtract(findSelectedOres.toSet())
        val defaultOthersSet = SuggestedEditsRecentEditsFilterTypes.DEFAULT_FILTER_OTHERS.map { it.id }.toSet()
        val nonDefaultOthers = defaultOthersSet.subtract(findSelectedOthers)
        return nonDefaultUserStatus.size + nonDefaultOthers.size + findSelectedOres.size
    }

    private fun latestRevisions(): String? {
        val includedTypesCodes = Prefs.recentEditsIncludedTypeCodes
        if (!includedTypesCodes.containsAll(SuggestedEditsRecentEditsFilterTypes.LATEST_REVISIONS_GROUP.map { it.id }) &&
            !includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.LATEST_REVISION.id)) {
            return SuggestedEditsRecentEditsFilterTypes.NOT_LATEST_REVISION.value
        }
        return null
    }

    private fun showCriteriaString(): String {
        val includedTypesCodes = Prefs.recentEditsIncludedTypeCodes
        val list = mutableListOf<String>()

        if (!includedTypesCodes.containsAll(SuggestedEditsRecentEditsFilterTypes.BOT_EDITS_GROUP.map { it.id })) {
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.BOT.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.BOT.value)
            }
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.HUMAN.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.HUMAN.value)
            }
        }

        if (!includedTypesCodes.containsAll(SuggestedEditsRecentEditsFilterTypes.MINOR_EDITS_GROUP.map { it.id })) {
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.MINOR_EDITS.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.MINOR_EDITS.value)
            }
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.NON_MINOR_EDITS.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.NON_MINOR_EDITS.value)
            }
        }

        if (includedTypesCodes.any { code ->
                SuggestedEditsRecentEditsFilterTypes.GOODFAITH_GROUP.map { it.id }.contains(code) ||
                        SuggestedEditsRecentEditsFilterTypes.DAMAGING_GROUP.map { it.id }.contains(code) }) {
            list.add("oresreview")
        }

        return list.joinToString(separator = "|")
    }

    inner class RecentEditsPagingSource : PagingSource<String, MwQueryResult.RecentChange>() {
        override suspend fun load(params: LoadParams<String>): LoadResult<String, MwQueryResult.RecentChange> {
            return try {

                val response = ServiceFactory.get(wikiSite)
                    .getRecentEdits(params.loadSize, Date().toInstant().toString(), latestRevisions(), showCriteriaString(), params.key)

                // Filtering Ores damaging and goodfaith
                val recentChanges = filterOresScores(filterOresScores(response.query?.recentChanges.orEmpty(), true), false)

                // Get usernames
                val usernames = recentChanges.filter { !it.anon }.map { it.user }.distinct().filter {
                    !cachedUserInfo.map { userInfo -> userInfo.name }.contains(it)
                }

                val usersInfoResponse = withContext(Dispatchers.IO) {
                    ServiceFactory.get(wikiSite).userInfo(usernames.joinToString(separator = "|"))
                }.query?.users ?: emptyList()

                cachedUserInfo.addAll(usersInfoResponse)

                // Filtering User experiences and registration.
                val finalRecentChanges = filterUserRegistration(filterUserExperience(recentChanges, cachedUserInfo))

                LoadResult.Page(finalRecentChanges, null, response.continuation?.rcContinuation)
            } catch (e: IOException) {
                LoadResult.Error(e)
            } catch (e: HttpException) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<String, MwQueryResult.RecentChange>): String? {
            return null
        }

        @Suppress("KotlinConstantConditions")
        private fun filterUserExperience(recentChanges: List<MwQueryResult.RecentChange>, usersInfo: List<UserInfo>): List<MwQueryResult.RecentChange> {
            val filterGroupSet = SuggestedEditsRecentEditsFilterTypes.USER_EXPERIENCE_GROUP.map { it.id }
            if (Prefs.recentEditsIncludedTypeCodes.any { code -> filterGroupSet.contains(code) }) {
                val findUserExperienceFilters = Prefs.recentEditsIncludedTypeCodes
                    .filter { code ->
                        filterGroupSet.contains(code)
                    }.map {
                        SuggestedEditsRecentEditsFilterTypes.find(it)
                    }
                // Filtering non-anon items with requirements and add anon items.
                return recentChanges.filter { !it.anon }.filter {
                    val userInfo = usersInfo.find { info -> info.name == it.user }
                    var qualifiedUser = false
                    userInfo?.let {
                        val editsCount = userInfo.editCount
                        val diffDays = diffDays(userInfo.registrationDate)
                        findUserExperienceFilters.forEach { type ->
                            val userExperienceArray = type.value.split("|")
                            val requiredEdits = userExperienceArray.first().split(",")
                            val requiredMinEdits = requiredEdits.first().toInt()
                            val requiredMaxEdits = requiredEdits.last().toInt()
                            val requiredLength = userExperienceArray.last().split(",")
                            val requiredMinLength = requiredLength.first().toLong()
                            val requiredMaxLength = requiredLength.last().toLong()

                            qualifiedUser = if (requiredMaxEdits == -1 && requiredMaxLength == -1L) {
                                editsCount >= requiredMinEdits && diffDays >= requiredMinLength
                            } else {
                                editsCount in requiredMinEdits..requiredMaxEdits && diffDays in requiredMinLength..requiredMaxLength
                            }
                            if (qualifiedUser) {
                                return@filter qualifiedUser
                            }
                        }
                    }
                    qualifiedUser
                } + recentChanges.filter { it.anon }
            }
            return recentChanges
        }

        private fun filterUserRegistration(recentChanges: List<MwQueryResult.RecentChange>): List<MwQueryResult.RecentChange> {
            val includedTypesCodes = Prefs.recentEditsIncludedTypeCodes
            val filterUserRegistrationGroupSet = SuggestedEditsRecentEditsFilterTypes.USER_REGISTRATION_GROUP.map { it.id }
            val filterUserExperiencesGroupSet = SuggestedEditsRecentEditsFilterTypes.USER_EXPERIENCE_GROUP.map { it.id }
            // 1. Skip when: both anon and non-anon selected; or anon and user experiences selected.
            if (!includedTypesCodes.containsAll(filterUserRegistrationGroupSet) &&
                !(includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.UNREGISTERED.id) &&
                        includedTypesCodes.any { filterUserExperiencesGroupSet.contains(it) })) {

                // 2. Filter anon items when only "UNREGISTERED" selected.
                if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.UNREGISTERED.id)) {
                    return recentChanges.filter { it.anon }
                }
                // 3. Filter non-anon items. E.g. registered or any user experiences selected.
                return recentChanges.filter { !it.anon }
            }
            return recentChanges
        }

        private fun filterOresScores(recentChanges: List<MwQueryResult.RecentChange>, isDamagingGroup: Boolean): List<MwQueryResult.RecentChange> {
            val filterGroupSet = if (isDamagingGroup) SuggestedEditsRecentEditsFilterTypes.DAMAGING_GROUP.map { it.id }
            else SuggestedEditsRecentEditsFilterTypes.GOODFAITH_GROUP.map { it.id }

            if (Prefs.recentEditsIncludedTypeCodes.any { code -> filterGroupSet.contains(code) }) {
                val findOresFilters = Prefs.recentEditsIncludedTypeCodes
                    .filter { code ->
                        filterGroupSet.contains(code)
                    }.map {
                        SuggestedEditsRecentEditsFilterTypes.find(it)
                    }

                return recentChanges.filter { it.ores != null }.filter {
                    val scoreRanges = findOresFilters.map { type -> type.value }
                    val oresScore = if (isDamagingGroup) it.ores?.damagingProb ?: 0f else it.ores?.goodfaithProb ?: 0f
                    scoreRanges.forEach { range ->
                        val scoreRangeArray = range.split("|")
                        val inScoreRange = oresScore >= scoreRangeArray.first().toFloat() && oresScore <= scoreRangeArray.last().toFloat()
                        if (inScoreRange) {
                            return@filter true
                        }
                    }
                    false
                }
            }
            return recentChanges
        }

        private fun diffDays(date: Date): Long {
            val nowDate = Calendar.getInstance().toInstant()
            val beginDate = date.toInstant()
            return Duration.between(beginDate, nowDate).toDays()
        }
    }

    open class RecentEditsItemModel
    class RecentEditsItem(val item: MwQueryResult.RecentChange) : RecentEditsItemModel()
    class RecentEditsSeparator(val date: String) : RecentEditsItemModel()
}

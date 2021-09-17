package org.wikipedia.userprofile

import android.content.Context
import android.icu.text.ListFormatter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DateUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.UserContributionFunnel
import org.wikipedia.analytics.eventplatform.UserContributionEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.FragmentContributionsSuggestedEditsBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.userprofile.Contribution.Companion.EDIT_TYPE_ARTICLE_DESCRIPTION
import org.wikipedia.userprofile.Contribution.Companion.EDIT_TYPE_GENERIC
import org.wikipedia.userprofile.Contribution.Companion.EDIT_TYPE_IMAGE_CAPTION
import org.wikipedia.userprofile.Contribution.Companion.EDIT_TYPE_IMAGE_TAG
import org.wikipedia.userprofile.ContributionsActivity.Companion.EXTRA_SOURCE_CONTRIBUTIONS
import org.wikipedia.userprofile.ContributionsActivity.Companion.EXTRA_SOURCE_PAGEVIEWS
import org.wikipedia.userprofile.ContributionsItemView.Callback
import org.wikipedia.util.DateUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultViewHolder
import java.util.*
import java.util.concurrent.TimeUnit

class ContributionsFragment : Fragment(), ContributionsHeaderView.Callback {
    private var _binding: FragmentContributionsSuggestedEditsBinding? = null
    private val binding get() = _binding!!
    private val adapter: ContributionsEntryItemAdapter = ContributionsEntryItemAdapter()

    private var allContributions = mutableListOf<Contribution>()
    private var displayedContributions = mutableListOf<Any>()

    private val disposables = CompositeDisposable()
    private val continuations = mutableMapOf<WikiSite, String>()

    private var editFilterType = EDIT_TYPE_GENERIC
    private var totalPageViews = 0L
    private var totalContributionCount = 0

    private val qNumberRegex = """Q(\d+)""".toRegex()
    private val commentRegex = """/\*.*?\*/""".toRegex()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        totalContributionCount = arguments?.getInt(EXTRA_SOURCE_CONTRIBUTIONS, 0)!!
        totalPageViews = arguments?.getLong(EXTRA_SOURCE_PAGEVIEWS, 0)!!
        _binding = FragmentContributionsSuggestedEditsBinding.inflate(LayoutInflater.from(context), container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.contributionsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.contributionsRecyclerView.adapter = adapter

        binding.contributionsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val scrollY = recyclerView.computeVerticalScrollOffset()
                val activity = requireActivity() as AppCompatActivity
                if (scrollY == 0 && activity.supportActionBar?.elevation != 0f) {
                    activity.supportActionBar?.elevation = 0f
                } else if (scrollY != 0 && activity.supportActionBar?.elevation == 0f) {
                    activity.supportActionBar?.elevation = DimenUtil.dpToPx(DimenUtil.getDimension(R.dimen.toolbar_default_elevation))
                }
            }
        })

        binding.swipeRefreshLayout.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
        binding.swipeRefreshLayout.setOnRefreshListener {
            resetAndFetch()
        }

        binding.errorView.backClickListener = View.OnClickListener {
            resetAndFetch()
        }

        resetAndFetch()

        UserContributionFunnel.get().logOpen()
        UserContributionEvent.logOpen()
    }

    override fun onDestroyView() {
        binding.contributionsRecyclerView.adapter = null
        binding.contributionsRecyclerView.clearOnScrollListeners()
        _binding = null
        disposables.clear()
        UserContributionFunnel.reset()
        super.onDestroyView()
    }

    override fun onTypeItemClick(editType: Int) {
        editFilterType = editType
        when (editFilterType) {
            EDIT_TYPE_ARTICLE_DESCRIPTION -> {
                UserContributionFunnel.get().logFilterDescriptions()
                UserContributionEvent.logFilterDescriptions()
            }
            EDIT_TYPE_IMAGE_CAPTION -> {
                UserContributionFunnel.get().logFilterCaptions()
                UserContributionEvent.logFilterCaptions()
            }
            EDIT_TYPE_IMAGE_TAG -> {
                UserContributionFunnel.get().logFilterTags()
                UserContributionEvent.logFilterTags()
            }
            else -> {
                UserContributionFunnel.get().logFilterAll()
                UserContributionEvent.logFilterAll()
            }
        }

        createConsolidatedList()
    }

    private fun resetAndFetch() {
        allContributions.clear()
        displayedContributions.clear()
        binding.errorView.visibility = GONE
        continuations.clear()
        adapter.notifyDataSetChanged()
        fetchContributions()
    }

    private fun fetchContributions() {
        if (allContributions.isNotEmpty() && continuations.isEmpty()) {
            // there's nothing more to fetch!
            return
        }

        binding.progressBar.visibility = VISIBLE
        disposables.clear()

        if (allContributions.isEmpty()) {
            disposables.add(UserContributionsStats.getPageViewsObservable().subscribe {
                totalPageViews = it
                adapter.notifyDataSetChanged()
            })
        }

        disposables.add(Observable.zip(homeSiteObservable(), wikiDataObservable(), wikiCommonsObservable(), {
                homeSiteContributions, wikidataContributions, commonsContributions ->
                    val totalContributionCount = homeSiteContributions.second + wikidataContributions.second + commonsContributions.second
                    val contributions = mutableListOf<Contribution>()
                    contributions.addAll(homeSiteContributions.first)
                    contributions.addAll(wikidataContributions.first)
                    contributions.addAll(commonsContributions.first)
                    Pair(contributions, totalContributionCount)
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.progressBar.visibility = GONE
                }
                .subscribe({
                    allContributions.addAll(it.first)
                    totalContributionCount = it.second
                    createConsolidatedList()
                }, { caught ->
                    L.e(caught)
                    showError(caught)
                }))
    }

    private fun homeSiteObservable(): Observable<Pair<List<Contribution>, Int>> {
        return if (allContributions.isNotEmpty() && !continuations.containsKey(WikipediaApp.instance.wikiSite)) Observable.just(Pair(Collections.emptyList(), -1))
        else ServiceFactory.get(WikipediaApp.instance.wikiSite).getUserContributions(AccountUtil.userName!!, 50, continuations[WikipediaApp.instance.wikiSite])
            .subscribeOn(Schedulers.io())
            .flatMap { response ->
                val contributions = mutableListOf<Contribution>()
                val cont = response.continuation["uccontinue"]
                if (cont.isNullOrEmpty()) {
                    continuations.remove(WikipediaApp.instance.wikiSite)
                } else {
                    continuations[WikipediaApp.instance.wikiSite] = cont
                }
                response.query?.userContributions?.forEach {
                    contributions.add(Contribution("", it.revid, it.title, it.title, it.title, EDIT_TYPE_GENERIC, null, it.date(),
                        WikipediaApp.instance.wikiSite, 0, it.sizediff, it.top, 0))
                }
                Observable.just(Pair(contributions, response.query?.userInfo!!.editCount))
            }
    }

    private fun wikiDataObservable(): Observable<Pair<List<Contribution>, Int>> {
        return if (allContributions.isNotEmpty() && !continuations.containsKey(WikiSite(Service.WIKIDATA_URL))) Observable.just(Pair(Collections.emptyList(), -1))
        else ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getUserContributions(AccountUtil.userName!!, 50, continuations[WikiSite(Service.WIKIDATA_URL)])
            .subscribeOn(Schedulers.io())
            .flatMap { response ->
                val wikidataContributions = mutableListOf<Contribution>()
                val qLangMap = hashMapOf<String, HashSet<String>>()
                val cont = response.continuation["uccontinue"]
                if (cont.isNullOrEmpty()) {
                    continuations.remove(WikiSite(Service.WIKIDATA_URL))
                } else {
                    continuations[WikiSite(Service.WIKIDATA_URL)] = cont
                }
                response.query?.userContributions?.forEach { contribution ->
                    var contributionLanguage = WikipediaApp.instance.appOrSystemLanguageCode
                    var contributionDescription = contribution.comment
                    var editType: Int = EDIT_TYPE_GENERIC
                    var qNumber = ""

                    val matches = commentRegex.findAll(contribution.comment)
                    if (matches.any()) {
                        val metaComment = deCommentString(matches.first().value)
                        if (metaComment.contains("wbsetdescription")) {
                            val descArr = metaComment.split("|")
                            if (descArr.size > 1) {
                                contributionLanguage = descArr[1]
                            }
                            editType = EDIT_TYPE_ARTICLE_DESCRIPTION
                            contributionDescription = extractDescriptionFromComment(contribution.comment, matches.first().value)
                        }
                    }

                    if (contribution.title.matches(qNumberRegex)) {
                        qNumber = contribution.title
                    }

                    if (qNumber.isNotEmpty() && !qLangMap.containsKey(qNumber)) {
                        qLangMap[qNumber] = HashSet()
                    }

                    wikidataContributions.add(Contribution(qNumber, contribution.revid, contribution.title, contribution.title, contributionDescription, editType, null, contribution.date(),
                        WikiSite.forLanguageCode(contributionLanguage), 0, contribution.sizediff, contribution.top, 0))

                    qLangMap[qNumber]?.add(contributionLanguage)
                }
                ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).getWikidataLabelsAndDescriptions(qLangMap.keys.joinToString("|"))
                    .subscribeOn(Schedulers.io())
                    .flatMap { entities ->
                        for (entityKey in entities.entities.keys) {
                            val entity = entities.entities[entityKey]!!
                            for (contribution in wikidataContributions) {
                                val dbName = WikiSite.forLanguageCode(contribution.wikiSite.languageCode).dbName()
                                if (contribution.qNumber == entityKey && entity.sitelinks.containsKey(dbName)) {
                                    contribution.apiTitle = entity.sitelinks[dbName]!!.title
                                    contribution.displayTitle = entity.sitelinks[dbName]!!.title
                                }
                            }
                        }
                        Observable.just(Pair(wikidataContributions, response.query?.userInfo!!.editCount))
                    }
            }
    }

    private fun wikiCommonsObservable(): Observable<Pair<List<Contribution>, Int>> {
        return if (allContributions.isNotEmpty() && !continuations.containsKey(WikiSite(Service.COMMONS_URL))) Observable.just(Pair(Collections.emptyList(), -1)) else
            ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getUserContributions(AccountUtil.userName!!, 200, continuations[WikiSite(Service.COMMONS_URL)])
                .subscribeOn(Schedulers.io())
                .flatMap { response ->
                    val contributions = mutableListOf<Contribution>()
                    val cont = response.continuation["uccontinue"]
                    if (cont.isNullOrEmpty()) {
                        continuations.remove(WikiSite(Service.COMMONS_URL))
                    } else {
                        continuations[WikiSite(Service.COMMONS_URL)] = cont
                    }
                    response.query?.userContributions?.forEach { contribution ->
                        var contributionLanguage = WikipediaApp.instance.appOrSystemLanguageCode
                        var editType: Int = EDIT_TYPE_GENERIC
                        var contributionDescription = contribution.comment
                        var qNumber = ""
                        var tagCount = 0

                        val matches = commentRegex.findAll(contribution.comment)
                        if (matches.any()) {
                            val metaComment = deCommentString(matches.first().value)

                            when {
                                metaComment.contains("wbsetlabel") -> {
                                    val descArr = metaComment.split("|")
                                    if (descArr.size > 1) {
                                        contributionLanguage = descArr[1]
                                    }
                                    editType = EDIT_TYPE_IMAGE_CAPTION
                                    contributionDescription = extractDescriptionFromComment(contribution.comment, matches.first().value)
                                }
                                metaComment.contains("wbsetclaim") -> {
                                    contributionDescription = ""
                                    qNumber = qNumberRegex.find(contribution.comment)?.value
                                        ?: ""
                                    editType = EDIT_TYPE_IMAGE_TAG
                                    tagCount = 1
                                }
                                metaComment.contains("wbeditentity") -> {
                                    if (matches.count() > 1 && matches.elementAt(1).value.contains(DEPICTS_META_STR)) {
                                        val metaContentStr = deCommentString(matches.elementAt(1).value)
                                        val map = extractTagsFromComment(metaContentStr)
                                        if (map.isNotEmpty()) {
                                            tagCount = map.size
                                            contributionDescription = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ListFormatter.getInstance().format(map.values) else map.values.joinToString(",")
                                        }
                                    }
                                    editType = EDIT_TYPE_IMAGE_TAG
                                }
                            }
                        }

                        contributions.add(Contribution(qNumber, contribution.revid, contribution.title, contribution.title, contributionDescription, editType, null, contribution.date(),
                            WikiSite.forLanguageCode(contributionLanguage), 0, contribution.sizediff, contribution.top, tagCount))
                    }
                    Observable.just(Pair(contributions, response.query?.userInfo!!.editCount))
                }
    }

    private fun createConsolidatedList() {
        displayedContributions.clear()
        val sortedContributions = mutableListOf<Contribution>()
        when (editFilterType) {
            EDIT_TYPE_ARTICLE_DESCRIPTION -> {
                sortedContributions.addAll(allContributions.filter { it.editType == EDIT_TYPE_ARTICLE_DESCRIPTION })
            }
            EDIT_TYPE_IMAGE_CAPTION -> {
                sortedContributions.addAll(allContributions.filter { it.editType == EDIT_TYPE_IMAGE_CAPTION })
            }
            EDIT_TYPE_IMAGE_TAG -> {
                sortedContributions.addAll(allContributions.filter { it.editType == EDIT_TYPE_IMAGE_TAG })
            }
            else -> {
                sortedContributions.addAll(allContributions)
            }
        }
        sortedContributions.sortWith { o2, o1 -> (o1.date.compareTo(o2.date)) }

        if (!sortedContributions.isNullOrEmpty()) {
            var currentDate = sortedContributions[0].date
            var nextDate: Date
            displayedContributions.add(getCorrectDateString(currentDate))
            for (position in 0 until sortedContributions.size) {
                nextDate = sortedContributions[position].date
                if (!DateUtils.isSameDay(nextDate, currentDate)) {
                    displayedContributions.add(getCorrectDateString(nextDate))
                    currentDate = nextDate
                }
                displayedContributions.add(sortedContributions[position])
            }
        }
        adapter.notifyDataSetChanged()
        binding.contributionsRecyclerView.visibility = VISIBLE
    }

    private fun getCorrectDateString(date: Date): String {
        val yesterday: Calendar = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return when {
            DateUtils.isSameDay(Calendar.getInstance().time, date) -> StringUtils.capitalize(getString(R.string.view_continue_reading_card_subtitle_today))
            DateUtils.isSameDay(yesterday.time, date) -> getString(R.string.suggested_edits_contribution_date_yesterday_text)
            else -> DateUtil.getFeedCardDateString(date)
        }
    }

    private fun deCommentString(str: String): String {
        return if (str.length < 4) str else str.substring(2, str.length - 2).trim()
    }

    @Suppress("SameParameterValue")
    private fun extractDescriptionFromComment(editComment: String, metaComment: String): String {
        var outStr = editComment.replace(metaComment, "")
        val hashtagPos = outStr.indexOf(", #suggestededit")
        if (hashtagPos >= 0) {
            outStr = outStr.substring(0, hashtagPos)
        }
        return outStr.trim()
    }

    private fun extractTagsFromComment(metaComment: String): HashMap<String, String> {
        val strArr = metaComment.replace(DEPICTS_META_STR, "").split(",")
        val outMap = hashMapOf<String, String>()
        for (item in strArr) {
            val itemArr = item.split("|")
            if (itemArr.size > 1) {
                outMap[itemArr[0]] = itemArr[1]
            }
        }
        return outMap
    }

    private fun showError(t: Throwable) {
        binding.swipeRefreshLayout.isRefreshing = false
        binding.contributionsRecyclerView.visibility = GONE
        binding.errorView.setError(t)
        binding.errorView.visibility = VISIBLE
    }

    private inner class HeaderViewHolder constructor(itemView: ContributionsHeaderView) : DefaultViewHolder<ContributionsHeaderView>(itemView) {
        fun bindItem() {
            view.callback = this@ContributionsFragment
            view.updateFilterViewUI(editFilterType, totalContributionCount)
            view.updateTotalPageViews(totalPageViews)
        }
    }

    private class DateViewHolder constructor(itemView: View) : DefaultViewHolder<View>(itemView) {
        init {
            itemView.setPaddingRelative(itemView.paddingStart, 0, itemView.paddingEnd, 0)
            itemView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DimenUtil.roundedDpToPx(32f))
        }

        var headerText: TextView = itemView.findViewById(R.id.section_header_text)
        fun bindItem(date: String) {
            headerText.text = date
        }
    }

    private inner class ContributionItemHolder constructor(itemView: ContributionsItemView) : DefaultViewHolder<ContributionsItemView>(itemView) {
        val disposables = CompositeDisposable()
        fun bindItem(contribution: Contribution) {
            view.contribution = contribution
            if (contribution.editType == EDIT_TYPE_GENERIC) {
                view.setTitle(contribution.displayTitle)
                view.setDescription(null)
            } else {
                view.setTitle(contribution.description)
                view.setDescription(StringUtil.removeNamespace(contribution.displayTitle))
            }
            view.setDiffCountText(contribution)
            view.setIcon(contribution.editType)
            view.setImageUrl(contribution.imageUrl)
            view.setPageViewCountText(contribution.pageViews)
            if (contribution.pageViews == 0L && contribution.editType == EDIT_TYPE_ARTICLE_DESCRIPTION) {
                getPageViews(view, contribution)
            }
            if (contribution.imageUrl == null) {
                getContributionDetails(view, contribution)
            }
        }

        fun clearDisposables() {
            disposables.clear()
        }

        private fun getContributionDetails(itemView: ContributionsItemView, contribution: Contribution) {
            if (contribution.editType == EDIT_TYPE_GENERIC ||
                (contribution.editType == EDIT_TYPE_ARTICLE_DESCRIPTION && contribution.apiTitle.isNotEmpty() && !contribution.apiTitle.matches(qNumberRegex))) {
                disposables.add(ServiceFactory.getRest(contribution.wikiSite).getSummary(null, contribution.apiTitle)
                        .subscribeOn(Schedulers.io())
                        .delaySubscription(250, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ summary ->
                            contribution.displayTitle = summary.displayTitle
                            contribution.apiTitle = summary.apiTitle
                            contribution.imageUrl = summary.thumbnailUrl.toString()
                            itemView.setImageUrl(contribution.imageUrl)
                            if (contribution.editType == EDIT_TYPE_ARTICLE_DESCRIPTION) {
                                itemView.setDescription(StringUtil.removeNamespace(contribution.displayTitle))
                            }
                        }, { t ->
                            L.e(t)
                        }))
            } else if (contribution.editType == EDIT_TYPE_IMAGE_CAPTION || contribution.editType == EDIT_TYPE_IMAGE_TAG) {
                disposables.add(Observable.zip(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo(contribution.apiTitle,
                    contribution.wikiSite.languageCode).subscribeOn(Schedulers.io()),
                    if (contribution.qNumber.isEmpty()) Observable.just(contribution.qNumber) else (
                            ServiceFactory.get(WikiSite(Service.WIKIDATA_URL))
                                .getWikidataLabels(contribution.qNumber, contribution.wikiSite.languageCode)
                                .subscribeOn(Schedulers.io())
                                .flatMap { response ->
                                    var label = contribution.qNumber
                                    val entities = response.entities
                                    val qNumber = entities[contribution.qNumber]
                                    qNumber?.let {
                                        if (it.labels.containsKey(contribution.wikiSite.languageCode)) {
                                            label = it.labels[contribution.wikiSite.languageCode]!!.value
                                        } else if (it.labels.containsKey(AppLanguageLookUpTable.FALLBACK_LANGUAGE_CODE)) {
                                            label = it.labels[AppLanguageLookUpTable.FALLBACK_LANGUAGE_CODE]!!.value
                                        }
                                    }
                                        Observable.just(label)
                                    }), { commonsResponse, qLabel ->

                            commonsResponse.query?.firstPage()?.imageInfo()?.let {
                                contribution.imageUrl = it.thumbUrl
                            } ?: run {
                                contribution.imageUrl = ""
                            }
                            if (contribution.editType == EDIT_TYPE_IMAGE_TAG && qLabel.isNotEmpty()) {
                                contribution.description = qLabel
                            }
                            contribution
                        })
                        .delaySubscription(250, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            itemView.setTitle(contribution.description)
                            itemView.setImageUrl(contribution.imageUrl)
                        })
            }
        }

        private fun getPageViews(view: ContributionsItemView, contribution: Contribution) {
            if (contribution.editType != EDIT_TYPE_ARTICLE_DESCRIPTION || contribution.apiTitle.matches(qNumberRegex)) {
                view.setPageViewCountText(0)
                return
            }
            disposables.add(ServiceFactory.get(contribution.wikiSite).getPageViewsForTitles(contribution.apiTitle)
                    .subscribeOn(Schedulers.io())
                    .delaySubscription(250, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        if (response is MwQueryResponse) {
                            contribution.pageViews = response.query?.pages?.sumOf { it.pageViewsMap?.values?.filterNotNull()?.sum()!! } ?: 0
                            view.setPageViewCountText(contribution.pageViews)
                        }
                    }) { t -> L.e(t) })
        }
    }

    inner class ContributionsEntryItemAdapter : RecyclerView.Adapter<DefaultViewHolder<*>>() {
        override fun getItemCount(): Int {
            return displayedContributions.size + 1
        }

        override fun getItemViewType(position: Int): Int {
            return when {
                position == 0 -> VIEW_TYPE_HEADER
                displayedContributions[position - 1] is String -> VIEW_TYPE_DATE
                else -> VIEW_TYPE_ITEM
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<*> {
            return when (viewType) {
                VIEW_TYPE_HEADER -> HeaderViewHolder(ContributionsHeaderView(parent.context))
                VIEW_TYPE_DATE -> DateViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_section_header, parent, false))
                else -> ContributionItemHolder(ContributionsItemView(parent.context))
            }
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, pos: Int) {
            when (holder) {
                is HeaderViewHolder -> holder.bindItem()
                is ContributionItemHolder -> holder.bindItem((displayedContributions[pos - 1] as Contribution))
                else -> (holder as DateViewHolder).bindItem((displayedContributions[pos - 1] as String))
            }
            if (displayedContributions.isNotEmpty() && pos >= displayedContributions.size - 1) {
                // If we have scrolled to the bottom, fetch the next batch of items.
                fetchContributions()
            }
        }

        override fun onViewAttachedToWindow(holder: DefaultViewHolder<*>) {
            super.onViewAttachedToWindow(holder)
            if (holder is ContributionItemHolder) {
                holder.view.callback = ItemCallback()
            }
        }

        override fun onViewDetachedFromWindow(holder: DefaultViewHolder<*>) {
            if (holder is ContributionItemHolder) {
                holder.view.callback = null
                holder.clearDisposables()
            }
            super.onViewDetachedFromWindow(holder)
        }
    }

    private class ItemCallback : Callback {
        override fun onClick(context: Context, contribution: Contribution) {
            when (contribution.editType) {
                EDIT_TYPE_ARTICLE_DESCRIPTION -> {
                    UserContributionFunnel.get().logViewDescription()
                    UserContributionEvent.logViewDescription()
                    context.startActivity(ContributionDetailsActivity.newIntent(context, contribution))
                }
                EDIT_TYPE_IMAGE_CAPTION -> {
                    UserContributionFunnel.get().logViewCaption()
                    UserContributionEvent.logViewCaption()
                    context.startActivity(ContributionDetailsActivity.newIntent(context, contribution))
                }
                EDIT_TYPE_IMAGE_TAG -> {
                    UserContributionFunnel.get().logViewTag()
                    UserContributionEvent.logViewTag()
                    context.startActivity(ContributionDetailsActivity.newIntent(context, contribution))
                }
                else -> {
                    UserContributionFunnel.get().logViewMisc()
                    UserContributionEvent.logViewMisc()
                    context.startActivity(ArticleEditDetailsActivity.newIntent(context, contribution.apiTitle, contribution.revId, contribution.wikiSite.languageCode))
                }
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_DATE = 1
        private const val VIEW_TYPE_ITEM = 2

        private const val DEPICTS_META_STR = "add-depicts:"

        fun newInstance(contributions: Int, pageViews: Long): ContributionsFragment {
            val fragment = ContributionsFragment()
            fragment.arguments = bundleOf(EXTRA_SOURCE_CONTRIBUTIONS to contributions,
                    EXTRA_SOURCE_PAGEVIEWS to pageViews)
            return fragment
        }
    }
}

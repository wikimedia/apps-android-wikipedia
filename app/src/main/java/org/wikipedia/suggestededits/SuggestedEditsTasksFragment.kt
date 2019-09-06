package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_tasks.*
import org.wikipedia.Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE
import org.wikipedia.Constants.InvokeSource.*
import org.wikipedia.Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.EditorTaskCounts
import org.wikipedia.language.LanguageSettingsInvokeSource
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.FooterMarginItemDecoration
import org.wikipedia.views.HeaderMarginItemDecoration
import java.util.*

class SuggestedEditsTasksFragment : Fragment() {
    private lateinit var addDescriptionsTask: SuggestedEditsTask
    private lateinit var translateDescriptionsTask: SuggestedEditsTask
    private lateinit var addImageCaptionsTask: SuggestedEditsTask
    private lateinit var translateImageCaptionsTask: SuggestedEditsTask
    private lateinit var multilingualTeaserTask: SuggestedEditsTask

    private val displayedTasks = ArrayList<SuggestedEditsTask>()
    private val callback = TaskViewCallback()

    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.elevation = 0f

        swipeRefreshLayout.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
        swipeRefreshLayout.setOnRefreshListener{ this.updateUI() }

        tasksRecyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val topDecorationDp = 16
        tasksRecyclerView.addItemDecoration(HeaderMarginItemDecoration(topDecorationDp, 0))
        tasksRecyclerView.addItemDecoration(FooterMarginItemDecoration(0, topDecorationDp))
        tasksRecyclerView.adapter = RecyclerAdapter(displayedTasks)

        usernameText.text = AccountUtil.getUserName()
        userContributionsButton.setOnClickListener {
            startActivity(SuggestedEditsContributionsActivity.newIntent(requireContext()))
        }
        setUpTasks()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_suggested_edits_tasks, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_help -> {
                FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
                super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private fun fetchUserContributions() {
        updateDisplayedTasks(null)
        contributionsText.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        disposables.add(ServiceFactory.get(WikiSite(Service.WIKIDATA_URL)).editorTaskCounts
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    progressBar.visibility = View.GONE
                    contributionsText.visibility = View.VISIBLE
                    swipeRefreshLayout.isRefreshing = false
                }
                .subscribe({ response ->
                    val editorTaskCounts = response.query()!!.editorTaskCounts()!!
                    var totalEdits = 0
                    for (count in editorTaskCounts.descriptionEditsPerLanguage.values) {
                        totalEdits += count
                    }
                    for (count in editorTaskCounts.captionEditsPerLanguage.values) {
                        totalEdits += count
                    }
                    contributionsText.text = resources.getQuantityString(R.plurals.suggested_edits_contribution_count, totalEdits, totalEdits)
                    updateDisplayedTasks(editorTaskCounts)
                }, { throwable ->
                    L.e(throwable)
                    FeedbackUtil.showError(requireActivity(), throwable)
                }))
    }

    private fun updateUI() {
        requireActivity().invalidateOptionsMenu()
        fetchUserContributions()
    }

    private fun setUpTasks() {
        addDescriptionsTask = SuggestedEditsTask()
        addDescriptionsTask.title = getString(R.string.suggested_edits_task_add_description_title)
        addDescriptionsTask.description = getString(R.string.suggested_edits_task_add_description_description)
        addDescriptionsTask.imageDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_short_text_white_24dp)

        translateDescriptionsTask = SuggestedEditsTask()
        translateDescriptionsTask.title = getString(R.string.suggested_edits_task_translation_title)
        translateDescriptionsTask.description = getString(R.string.suggested_edits_task_translation_description)
        translateDescriptionsTask.imageDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_icon_translate_title_descriptions)

        addImageCaptionsTask = SuggestedEditsTask()
        addImageCaptionsTask.title = getString(R.string.suggested_edits_task_image_caption_title)
        addImageCaptionsTask.description = getString(R.string.suggested_edits_task_image_caption_description)
        addImageCaptionsTask.imageDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_icon_caption_images)

        translateImageCaptionsTask = SuggestedEditsTask()
        translateImageCaptionsTask.title = getString(R.string.suggested_edits_task_translate_caption_title)
        translateImageCaptionsTask.description = getString(R.string.suggested_edits_task_translate_caption_description)
        translateImageCaptionsTask.imageDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_icon_caption_translate)

        multilingualTeaserTask = SuggestedEditsTask()
        multilingualTeaserTask.title = getString(R.string.suggested_edits_task_multilingual_title)
        multilingualTeaserTask.description = getString(R.string.suggested_edits_task_multilingual_description)
        multilingualTeaserTask.showImagePlaceholder = false
        multilingualTeaserTask.showActionLayout = true
        multilingualTeaserTask.unlockActionPositiveButtonString = getString(R.string.suggested_edits_task_multilingual_positive)
        multilingualTeaserTask.unlockActionNegativeButtonString = getString(R.string.suggested_edits_task_multilingual_negative)
    }

    private fun updateDisplayedTasks(editorTaskCounts: EditorTaskCounts?) {
        displayedTasks.clear()
        try {
            if (editorTaskCounts == null) {
                return
            }

            val targetForAddDescriptions = editorTaskCounts.descriptionEditTargets[0]
            val targetForTranslateDescriptions = editorTaskCounts.descriptionEditTargets[1]

            displayedTasks.add(addDescriptionsTask)
            addDescriptionsTask.unlockMessageText = String.format(getString(R.string.suggested_edits_task_translate_description_edit_disable_text), targetForAddDescriptions)

            if (WikipediaApp.getInstance().language().appLanguageCodes.size >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION) {
                displayedTasks.add(translateDescriptionsTask)
                translateDescriptionsTask.unlockMessageText = String.format(getString(R.string.suggested_edits_task_translate_description_edit_disable_text), targetForTranslateDescriptions)
            }

            val targetForAddCaptions = editorTaskCounts.captionEditTargets[0]
            val targetForTranslateCaptions = editorTaskCounts.captionEditTargets[1]

            displayedTasks.add(addImageCaptionsTask)
            addImageCaptionsTask.unlockMessageText = String.format(getString(R.string.suggested_edits_task_image_caption_edit_disable_text), targetForAddCaptions)

            if (WikipediaApp.getInstance().language().appLanguageCodes.size >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION) {
                displayedTasks.add(translateImageCaptionsTask)
                translateImageCaptionsTask.unlockMessageText = String.format(getString(R.string.suggested_edits_task_image_caption_edit_disable_text), targetForTranslateCaptions)
                translateImageCaptionsTask.showActionLayout = false
                translateImageCaptionsTask.disabled = false
            }

            if (WikipediaApp.getInstance().language().appLanguageCodes.size < MIN_LANGUAGES_TO_UNLOCK_TRANSLATION && Prefs.showSuggestedEditsMultilingualTeaserTask()) {
                displayedTasks.add(multilingualTeaserTask)
                multilingualTeaserTask.showActionLayout = true
                multilingualTeaserTask.disabled = false
            }

        } catch (e: Exception) {
            L.e(e)
        } finally {
            tasksRecyclerView.adapter!!.notifyDataSetChanged()
        }
    }

    private inner class TaskViewCallback : SuggestedEditsTaskView.Callback {
        override fun onPositiveActionClick(task: SuggestedEditsTask) {
            if (task == multilingualTeaserTask) {
                requireActivity().startActivityForResult(WikipediaLanguagesActivity.newIntent(requireActivity(),
                        LanguageSettingsInvokeSource.DESCRIPTION_EDITING.text()), ACTIVITY_REQUEST_ADD_A_LANGUAGE)
            }
        }

        override fun onNegativeActionClick(task: SuggestedEditsTask) {
            if (task == multilingualTeaserTask) {
                val multilingualTaskPosition = displayedTasks.indexOf(multilingualTeaserTask)
                displayedTasks.remove(multilingualTeaserTask)
                tasksRecyclerView.adapter!!.notifyItemChanged(multilingualTaskPosition)
                Prefs.setShowSuggestedEditsMultilingualTeaserTask(false)
            }
        }

        override fun onViewClick(task: SuggestedEditsTask) {
            if (task == addDescriptionsTask) {
                startActivity(SuggestedEditsCardsActivity.newIntent(requireActivity(), SUGGESTED_EDITS_ADD_DESC))
            } else if (task == addImageCaptionsTask) {
                startActivity(SuggestedEditsCardsActivity.newIntent(requireActivity(), SUGGESTED_EDITS_ADD_CAPTION))
            } else if (task == translateDescriptionsTask && WikipediaApp.getInstance().language().appLanguageCodes.size > 1) {
                startActivity(SuggestedEditsCardsActivity.newIntent(requireActivity(), SUGGESTED_EDITS_TRANSLATE_DESC))
            } else if (task == translateImageCaptionsTask && WikipediaApp.getInstance().language().appLanguageCodes.size > 1) {
                startActivity(SuggestedEditsCardsActivity.newIntent(requireActivity(), SUGGESTED_EDITS_TRANSLATE_CAPTION))
            }
        }
    }

    internal inner class RecyclerAdapter(tasks: List<SuggestedEditsTask>) : DefaultRecyclerAdapter<SuggestedEditsTask, SuggestedEditsTaskView>(tasks) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<SuggestedEditsTaskView> {
            return DefaultViewHolder(SuggestedEditsTaskView(parent.context))
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<SuggestedEditsTaskView>, i: Int) {
            holder.view.setUpViews(items()[i], callback)
        }
    }

    companion object {
        fun newInstance(): SuggestedEditsTasksFragment {
            return SuggestedEditsTasksFragment()
        }
    }
}

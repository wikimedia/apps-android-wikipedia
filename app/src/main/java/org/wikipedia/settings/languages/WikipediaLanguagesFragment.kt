package org.wikipedia.settings.languages

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.FragmentWikipediaLanguagesBinding
import org.wikipedia.extensions.serializableExtra
import org.wikipedia.json.JsonUtil
import org.wikipedia.language.LanguagesListActivity
import org.wikipedia.push.WikipediaFirebaseMessagingService
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.MultiSelectActionModeCallback
import java.util.*

class WikipediaLanguagesFragment : Fragment(), MenuProvider, WikipediaLanguagesItemView.Callback {
    private var _binding: FragmentWikipediaLanguagesBinding? = null
    private val binding get() = _binding!!
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var adapter: WikipediaLanguageItemAdapter
    private lateinit var invokeSource: InvokeSource
    private lateinit var initialLanguageList: String
    private var app: WikipediaApp = WikipediaApp.instance
    private val wikipediaLanguages = mutableListOf<String>()
    private val selectedCodes = mutableListOf<String>()
    private var actionMode: ActionMode? = null
    private val multiSelectCallback: MultiSelectCallback = MultiSelectCallback()
    private var interactionsCount = 0
    private var isLanguageSearched = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWikipediaLanguagesBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        invokeSource = requireActivity().intent.serializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE)!!
        initialLanguageList = JsonUtil.encodeToString(app.languageState.appLanguageCodes).orEmpty()
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        prepareWikipediaLanguagesList()
        setupRecyclerView()
        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE && resultCode == Activity.RESULT_OK) {
            interactionsCount += data!!.getIntExtra(ADD_LANGUAGE_INTERACTIONS, 0)
            isLanguageSearched = isLanguageSearched || data.getBooleanExtra(LanguagesListActivity.LANGUAGE_SEARCHED, false)
            prepareWikipediaLanguagesList()
            requireActivity().invalidateOptionsMenu()
            adapter.notifyDataSetChanged()
            // explicitly update notification subscription options for any new language wikis
            Prefs.isPushNotificationOptionsSet = false
            WikipediaFirebaseMessagingService.updateSubscription()
        }
    }

    override fun onDestroyView() {
        binding.wikipediaLanguagesRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_wikipedia_languages, menu)
        if (app.languageState.appLanguageCodes.size <= 1) {
            val overflowMenu = menu.getItem(0)
            overflowMenu.isVisible = false
        }
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_wikipedia_languages_remove -> {
                beginRemoveLanguageMode()
                true
            }
            else -> false
        }
    }

    override fun onCheckedChanged(position: Int) {
        toggleSelectedLanguage(wikipediaLanguages[position])
    }

    override fun onLongPress(position: Int) {
        if (actionMode == null) {
            beginRemoveLanguageMode()
        }
        toggleSelectedLanguage(wikipediaLanguages[position])
        adapter.notifyDataSetChanged()
    }

    private fun prepareWikipediaLanguagesList() {
        wikipediaLanguages.clear()
        wikipediaLanguages.addAll(app.languageState.appLanguageCodes)
    }

    private fun setupRecyclerView() {
        binding.wikipediaLanguagesRecycler.setHasFixedSize(true)
        adapter = WikipediaLanguageItemAdapter()
        binding.wikipediaLanguagesRecycler.adapter = adapter
        binding.wikipediaLanguagesRecycler.layoutManager = LinearLayoutManager(activity)
        itemTouchHelper = ItemTouchHelper(RearrangeableItemTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.wikipediaLanguagesRecycler)
    }

    private fun updateWikipediaLanguages() {
        app.languageState.setAppLanguageCodes(wikipediaLanguages)
        adapter.notifyDataSetChanged()
        requireActivity().invalidateOptionsMenu()
    }

    private inner class WikipediaLanguageItemAdapter : RecyclerView.Adapter<DefaultViewHolder<*>>() {
        private var checkboxEnabled = false
        override fun getItemViewType(position: Int): Int {
            return when (position) {
                0 -> { VIEW_TYPE_HEADER }
                itemCount - 1 -> { VIEW_TYPE_FOOTER }
                else -> { VIEW_TYPE_ITEM }
            }
        }

        override fun getItemCount(): Int {
            return wikipediaLanguages.size + NUM_HEADERS + NUM_FOOTERS
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<*> {
            return when (viewType) {
                Companion.VIEW_TYPE_HEADER -> {
                    HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_section_header, parent, false))
                }
                Companion.VIEW_TYPE_FOOTER -> {
                    FooterViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_wikipedia_language_footer, parent, false))
                }
                else -> {
                    WikipediaLanguageItemHolder(WikipediaLanguagesItemView(parent.context))
                }
            }
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, pos: Int) {
            if (holder is WikipediaLanguageItemHolder) {
                holder.bindItem(wikipediaLanguages[pos - NUM_HEADERS], pos - NUM_FOOTERS)
                holder.view.setCheckBoxEnabled(checkboxEnabled)
                holder.view.setCheckBoxChecked(selectedCodes.contains(wikipediaLanguages[pos - NUM_HEADERS]))
                holder.view.setDragHandleEnabled(wikipediaLanguages.size > 1 && !checkboxEnabled)
                holder.view.setOnClickListener {
                    if (actionMode != null) {
                        toggleSelectedLanguage(wikipediaLanguages[pos - NUM_HEADERS])
                        adapter.notifyDataSetChanged()
                    } else if (wantResultFromItemClick()) {
                        Intent().let {
                            it.putExtra(ACTIVITY_RESULT_LANG_POSITION_DATA, pos - NUM_HEADERS)
                            requireActivity().setResult(Activity.RESULT_OK, it)
                            requireActivity().finish()
                        }
                    }
                }
            }
        }

        override fun onViewAttachedToWindow(holder: DefaultViewHolder<*>) {
            super.onViewAttachedToWindow(holder)
            if (holder is WikipediaLanguageItemHolder) {
                holder.view.setDragHandleTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            interactionsCount++
                            itemTouchHelper.startDrag(holder)
                        }
                        MotionEvent.ACTION_UP -> v.performClick()
                        else -> { }
                    }
                    false
                }
                holder.view.callback = this@WikipediaLanguagesFragment
            } else if (holder is FooterViewHolder) {
                holder.view.visibility = if (checkboxEnabled) View.GONE else View.VISIBLE
                holder.view.setOnClickListener {
                    Intent(requireActivity(), LanguagesListActivity::class.java).let {
                        startActivityForResult(it, Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE)
                        actionMode?.finish()
                    }
                }
            }
        }

        override fun onViewDetachedFromWindow(holder: DefaultViewHolder<*>) {
            if (holder is WikipediaLanguageItemHolder) {
                holder.view.callback = null
                holder.view.setDragHandleTouchListener(null)
            }
            super.onViewDetachedFromWindow(holder)
        }

        fun onMoveItem(oldPosition: Int, newPosition: Int) {
            Collections.swap(wikipediaLanguages, oldPosition - NUM_HEADERS, newPosition - NUM_FOOTERS)
            notifyItemMoved(oldPosition, newPosition)
        }

        fun onCheckboxEnabled(enabled: Boolean) {
            checkboxEnabled = enabled
        }
    }

    private inner class RearrangeableItemTouchHelperCallback(private val adapter: WikipediaLanguageItemAdapter) : ItemTouchHelper.Callback() {
        override fun isLongPressDragEnabled(): Boolean {
            return false
        }

        override fun isItemViewSwipeEnabled(): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return if (viewHolder is WikipediaLanguageItemHolder) makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) else -1
        }

        override fun onMove(recyclerView: RecyclerView, source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            if (target is WikipediaLanguageItemHolder) {
                adapter.onMoveItem(source.bindingAdapterPosition, target.bindingAdapterPosition)
            }
            return true
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            recyclerView.post {
                if (isAdded) {
                    updateWikipediaLanguages()
                }
            }
        }
    }

    private inner class HeaderViewHolder(itemView: View) : DefaultViewHolder<View>(itemView) {
        init {
            itemView.findViewById<TextView>(R.id.section_header_text).setText(R.string.wikipedia_languages_your_languages_text)
        }
    }

    private inner class WikipediaLanguageItemHolder(itemView: WikipediaLanguagesItemView) : DefaultViewHolder<WikipediaLanguagesItemView>(itemView) {
        fun bindItem(languageCode: String, position: Int) {
            view.setContents(languageCode, app.languageState.getAppLanguageCanonicalName(languageCode), position)
        }
    }

    private inner class FooterViewHolder(itemView: View) : DefaultViewHolder<View>(itemView)

    private fun wantResultFromItemClick(): Boolean {
        val source = requireActivity().intent.serializableExtra<InvokeSource>(Constants.INTENT_EXTRA_INVOKE_SOURCE)
        return source != null && (source == InvokeSource.SEARCH || source == InvokeSource.TALK_TOPICS_ACTIVITY || source == InvokeSource.USER_CONTRIB_ACTIVITY)
    }

    private fun setMultiSelectEnabled(enabled: Boolean) {
        adapter.onCheckboxEnabled(enabled)
        adapter.notifyDataSetChanged()
        requireActivity().invalidateOptionsMenu()
    }

    private fun beginRemoveLanguageMode() {
        (requireActivity() as AppCompatActivity).startSupportActionMode(multiSelectCallback)
        setMultiSelectEnabled(true)
    }

    private fun toggleSelectedLanguage(code: String) {
        if (selectedCodes.contains(code)) {
            selectedCodes.remove(code)
        } else {
            selectedCodes.add(code)
        }
    }

    private fun unselectAllLanguages() {
        selectedCodes.clear()
        adapter.notifyDataSetChanged()
    }

    private fun deleteSelectedLanguages() {
        app.languageState.removeAppLanguageCodes(selectedCodes)
        interactionsCount++
        prepareWikipediaLanguagesList()
        unselectAllLanguages()
    }

    private inner class MultiSelectCallback : MultiSelectActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            super.onCreateActionMode(mode, menu)
            mode.setTitle(R.string.wikipedia_languages_remove_action_mode_title)
            mode.menuInflater.inflate(R.menu.menu_action_mode_wikipedia_languages, menu)
            actionMode = mode
            selectedCodes.clear()
            return super.onCreateActionMode(mode, menu)
        }

        override fun onDeleteSelected() {
            showRemoveLanguagesDialog()
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            unselectAllLanguages()
            setMultiSelectEnabled(false)
            actionMode = null
            super.onDestroyActionMode(mode)
        }
    }

    fun showRemoveLanguagesDialog() {
        if (selectedCodes.size > 0) {
            MaterialAlertDialogBuilder(requireActivity()).let {
                if (selectedCodes.size < wikipediaLanguages.size) {
                    it
                    .setTitle(resources.getQuantityString(R.plurals.wikipedia_languages_remove_dialog_title, selectedCodes.size))
                    .setMessage(R.string.wikipedia_languages_remove_dialog_content)
                    .setPositiveButton(R.string.remove_language_dialog_ok_button_text) { _, _ ->
                        deleteSelectedLanguages()
                        actionMode?.finish()
                        requireActivity().setResult(SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED)
                    }
                    .setNegativeButton(R.string.remove_language_dialog_cancel_button_text, null)
                } else {
                    it
                    .setTitle(R.string.wikipedia_languages_remove_warning_dialog_title)
                    .setMessage(R.string.wikipedia_languages_remove_warning_dialog_content)
                    .setPositiveButton(R.string.remove_all_language_warning_dialog_ok_button_text, null)
                }
                it.show()
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_FOOTER = 2
        private const val NUM_HEADERS = 1
        private const val NUM_FOOTERS = 1
        const val ACTIVITY_RESULT_LANG_POSITION_DATA = "activity_result_lang_position_data"
        const val ADD_LANGUAGE_INTERACTIONS = "add_language_interactions"
        const val SESSION_TOKEN = "session_token"

        fun newInstance(invokeSource: InvokeSource): WikipediaLanguagesFragment {
            val instance = WikipediaLanguagesFragment()
            instance.arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            return instance
        }
    }
}

package org.wikipedia.views

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.ResourceCursorAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.DialogWikiWrappedBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.history.HistoryEntry
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L

class WikiWrappedDialog(activity: Activity) : MaterialAlertDialogBuilder(activity) {
    private val binding = DialogWikiWrappedBinding.inflate(activity.layoutInflater)
    private var dialog: AlertDialog? = null
    private val disposables = CompositeDisposable()
    private val finalWrappedList = mutableListOf<WrappedObject>()

    init {
        setView(binding.root)
        Glide.with(context)
            .load(R.raw.wrapped_shapes)
            .into(DrawableImageViewTarget(binding.wrappedGifView))

        CoroutineScope(Dispatchers.Main).launch {
            try {
                fetchListOfTopics(CoroutineScope(Dispatchers.Main))
            } catch (e: Exception) {
                L.e(e)
            }
        }
        binding.wrappedRecycler.layoutManager = LinearLayoutManager(context)
        binding.wrappedRecycler.adapter = CustomWrappedAdapter(mutableListOf(), activity)
    }

    private fun onLoadItemsFinished(readingTopics: List<String>, editingTopics: List<String>, editCount: Int) {

        finalWrappedList.add(
            WrappedObject(
                context.getString(R.string.articles_stat_string),
                readingTopics
            )
        )
        finalWrappedList.add(
            WrappedObject(
                context.getString(R.string.edits_stat_string, editCount),
                emptyList()
            )
        )
        finalWrappedList.add(
            WrappedObject(
                context.getString(R.string.contributions_stat_string),
                editingTopics
            )
        )
    }

    private suspend fun fetchListOfTopics(scope: CoroutineScope) {

        withContext(Dispatchers.IO) {

            val readingTopicsTask = async {
                val historyItems =
                    AppDatabase.instance.historyEntryWithImageDao().findEntriesBySearchTerm("%%")
                        .take(50)
                        .map { it.apiTitle }

                val response = ServiceFactory.get(WikipediaApp.instance.wikiSite)
                    .getCirrusDocData(historyItems.joinToString("|"))

                val topicMap = mutableMapOf<String, Int>()

                response.query?.pages?.forEach { page ->
                    var haveAsterisks = false

                    var topics =
                        page.cirrusdoc?.get(0)?.source?.ores_articletopics.orEmpty()
                            .filter { it.startsWith("articletopic/") }
                            .map {
                                if (it.contains("*")) haveAsterisks =
                                    true; it.substringAfter("articletopic/")
                            }

                    if (haveAsterisks) {
                        topics = topics.filter { it.contains("*") }.map { it.replace("*", "") }
                    }

                    topics = topics.sortedBy { it.split("|")[1].toInt() }
                        .map { it.split("|")[0] }
                        .map { if (it.contains(".")) it.split(".").last() else it }

                    topics.forEach {
                        if (topicMap.containsKey(it)) {
                            topicMap[it] = topicMap[it]!! + 1
                        } else {
                            topicMap[it] = 1
                        }
                    }
                }

                topicMap.forEach {
                    L.d(">>>> " + it)
                }
                topicMap
            }

            var totalEditCount = 0

            val editTopicsTask = async {
                val contribResponse = ServiceFactory.get(WikipediaApp.instance.wikiSite).
                    getUserContributions(AccountUtil.userName.orEmpty(), 100, null)

                val editedTitles = (contribResponse.query?.userContributions?.map { it.title } ?: emptyList<String>())
                    .take(50)

                totalEditCount = contribResponse.query?.userInfo?.editCount ?: 0

                val response = ServiceFactory.get(WikipediaApp.instance.wikiSite)
                    .getCirrusDocData(editedTitles.joinToString("|"))

                val topicMap = mutableMapOf<String, Int>()

                response.query?.pages?.forEach { page ->
                    var haveAsterisks = false

                    var topics =
                        page.cirrusdoc?.get(0)?.source?.ores_articletopics.orEmpty()
                            .filter { it.startsWith("articletopic/") }
                            .map {
                                if (it.contains("*")) haveAsterisks =
                                    true; it.substringAfter("articletopic/")
                            }

                    if (haveAsterisks) {
                        topics = topics.filter { it.contains("*") }.map { it.replace("*", "") }
                    }

                    topics = topics.sortedBy { it.split("|")[1].toInt() }
                        .map { it.split("|")[0] }
                        .map { if (it.contains(".")) it.split(".").last() else it }

                    topics.forEach {
                        if (topicMap.containsKey(it)) {
                            topicMap[it] = topicMap[it]!! + 1
                        } else {
                            topicMap[it] = 1
                        }
                    }
                }

                topicMap.forEach {
                    L.d(">>>> " + it)
                }
                topicMap
            }

            val readingTopics = readingTopicsTask.await()
            val editingTopics = editTopicsTask.await()

            L.d(">>> " + readingTopics)
            L.d(">>> " + editingTopics)





            onLoadItemsFinished(readingTopics.keys.map { key ->
                    key + " (" + readingTopics[key] + ")"
                },
                editingTopics.keys.map { key ->
                    key + " (" + editingTopics[key] + ")"
                }, totalEditCount)
        }


        scope.launch {
            finalWrappedList.forEach {
                (binding.wrappedRecycler.adapter as CustomWrappedAdapter).addToList(it)
                delay(2000)
            }
        }
    }

    class CustomWrappedAdapter(
        private val items: MutableList<WrappedObject>, private val context: Context
    ) :
        RecyclerView.Adapter<CustomWrappedAdapter.ViewHolder>() {

        private var lastPosition = -1

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var text: TextView

            var container: LinearLayout
            var chipGroup: ChipGroup

            init {
                container = itemView.findViewById<View>(R.id.item_layout_container) as LinearLayout
                text = itemView.findViewById<View>(R.id.item_layout_text) as TextView
                chipGroup = itemView.findViewById<View>(R.id.wrappedChipGroup) as ChipGroup
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_wrapped_item, parent, false)
            return ViewHolder(v)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.text.text = items[position].statString

            holder.chipGroup.setSingleLine(true)
            holder.chipGroup.isVisible = items[position].chipStringList.isNotEmpty()
            if (items[position].chipStringList.isNotEmpty()) {
                items[position].chipStringList.forEach {
                    val chip = Chip(context)
                    chip.text = it
                    chip.isCloseIconVisible = false
                    chip.setTextColor(ResourceUtil.getThemedColor(context, R.attr.primary_color))

                    holder.chipGroup.addView(chip)
                }
            }
            setAnimation(holder.itemView, position)
        }

        private fun setAnimation(viewToAnimate: View, position: Int) {
            // If the bound view wasn't previously displayed on screen, it's animated
            if (position > lastPosition) {
                val animation: Animation =
                    AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
                viewToAnimate.startAnimation(animation)
                lastPosition = position
            }
        }

        fun addToList(item: WrappedObject) {
            items.add(item)
            notifyDataSetChanged()
        }
    }

    class WrappedObject(val statString: String, val chipStringList: List<String>)

    override fun show(): AlertDialog {
        dialog = super.show()
        return dialog!!
    }
}

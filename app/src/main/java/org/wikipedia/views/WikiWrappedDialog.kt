package org.wikipedia.views

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.DialogWikiWrappedBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.util.log.L

class WikiWrappedDialog(activity: Activity) : MaterialAlertDialogBuilder(activity) {
    private val binding = DialogWikiWrappedBinding.inflate(activity.layoutInflater)
    private var dialog: AlertDialog? = null
    private val disposables = CompositeDisposable()

    init {
        setView(binding.root)
        Glide.with(context)
            .load(R.raw.wrapped_shapes)
            .into(DrawableImageViewTarget(binding.wrappedGifView))

        binding.wrappedRecycler.layoutManager = LinearLayoutManager(context)
        binding.wrappedRecycler.adapter =
            CustomWrappedAdapter(mutableListOf(wrappedList[0]), activity)

        runBlocking { fetchListOfTopics(CoroutineScope(Dispatchers.Main)) }
    }

    private suspend fun fetchListOfTopics(scope: CoroutineScope) {

        withContext(Dispatchers.IO) {

            val historyItems = AppDatabase.instance.historyEntryWithImageDao().findEntriesBySearchTerm("%%")
                .map { it.apiTitle }

            val response = ServiceFactory.get(WikipediaApp.instance.wikiSite).getCirrusDocData(historyItems.joinToString("|"))

            val topicSet = mutableSetOf<String>()

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
                    topicSet.add(it)
                }
            }

            topicSet.forEach {
                L.d(">>>> " + it)
            }
        }


        scope.launch {
            delay(2000)
            for (i in 1 until wrappedList.size) {
                (binding.wrappedRecycler.adapter as CustomWrappedAdapter).addToList(wrappedList[i])
                delay(2000)
            }
        }
    }

    class CustomWrappedAdapter(
        private val items: MutableList<String>, private val context: Context
    ) :
        RecyclerView.Adapter<CustomWrappedAdapter.ViewHolder>() {

        private var lastPosition = -1

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var text: TextView

            var container: FrameLayout

            init {
                container = itemView.findViewById<View>(R.id.item_layout_container) as FrameLayout
                text = itemView.findViewById<View>(R.id.item_layout_text) as TextView
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
            holder.text.text = items[position]

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

        fun addToList(item: String) {
            items.add(item)
            notifyDataSetChanged()
        }
    }

    override fun show(): AlertDialog {
        dialog = super.show()
        return dialog!!
    }

    companion object {
        val wrappedList = mutableListOf(
            "Articles", "Edits", "Contributions"
        )
    }
}

package org.wikipedia.topics

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityTopicsBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ResourceUtil

class TopicsActivity : BaseActivity() {
    private lateinit var binding: ActivityTopicsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopicsBinding.inflate(layoutInflater)
        binding.topicsListRecycler.layoutManager = GridLayoutManager(this, 2)
        binding.topicsListRecycler.adapter = CustomAdapter(this)
        setContentView(binding.root)
    }

    class CustomAdapter(val context: Context) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {
        val dataSet: Array<String> = context.resources.getStringArray(R.array.topics)
        var topics = Prefs.selectedTopics.toMutableSet()

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(R.id.textView)
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(viewGroup.context).inflate(R.layout.view_topic_item, viewGroup, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            viewHolder.textView.text = dataSet[position]
            if (topics.contains(dataSet[position])) {
                viewHolder.textView.setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.color_group_70))
            }
            viewHolder.textView.setOnClickListener {
                val topic = (it as TextView).text.toString()
                if (topics.contains(topic)) {
                    topics.remove(topic)
                } else {
                    topics.add(topic)
                }
                notifyDataSetChanged()
                Prefs.selectedTopics = topics
            }
        }

        override fun getItemCount() = dataSet.size
    }
}

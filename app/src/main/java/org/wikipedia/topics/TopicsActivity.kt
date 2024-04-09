package org.wikipedia.topics

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
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
            val chip: Chip = view.findViewById(R.id.topicName)
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(viewGroup.context).inflate(R.layout.view_topic_item, viewGroup, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            viewHolder.chip.text = dataSet[position]
            if (topics.contains(dataSet[position])) {
                viewHolder.chip.isChecked = true
            } else {
                viewHolder.chip.isChecked = false
            }
            viewHolder.chip.setOnClickListener {
                val topic = (it as Chip).text.toString()
                if (topics.contains(topic)) {
                    topics.remove(topic)
                } else {
                    topics.add(topic)
                }
                Prefs.selectedTopics = topics
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = dataSet.size
    }
}

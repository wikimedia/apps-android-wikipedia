package org.wikipedia.topics

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityTopicsBinding
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

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(R.id.textView)
        }

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view =
                LayoutInflater.from(viewGroup.context).inflate(R.layout.view_topic_item, viewGroup, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            viewHolder.textView.text = dataSet[position]
            viewHolder.textView.background = AppCompatResources.getDrawable(context, ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackground))
        }

        override fun getItemCount() = dataSet.size
    }
}

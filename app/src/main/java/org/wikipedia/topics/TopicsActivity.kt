package org.wikipedia.topics

import android.os.Bundle
import android.util.TypedValue
import com.google.android.material.chip.Chip
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityTopicsBinding
import org.wikipedia.settings.Prefs

class TopicsActivity : BaseActivity() {
    private lateinit var binding: ActivityTopicsBinding

    private lateinit var allTopics: Array<String>
    private var topics = Prefs.selectedTopics.toMutableSet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopicsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        allTopics = resources.getStringArray(R.array.topics)

        allTopics.forEach {
            val chip = Chip(this)
            chip.text = it
            chip.isCheckable = true
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            chip.setCheckedIconResource(R.drawable.checked)
            chip.isCheckedIconVisible = true
            binding.chipGroup.addView(chip)
            if (topics.contains(it)) {
                chip.isChecked = true
            }

            chip.setOnClickListener {
                val topic = (it as Chip).text.toString()
                if (topics.contains(topic)) {
                    topics.remove(topic)
                } else {
                    topics.add(topic)
                }
                Prefs.selectedTopics = topics
            }
        }
    }
}

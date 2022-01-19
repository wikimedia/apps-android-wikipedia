package org.wikipedia.page

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import org.wikipedia.databinding.ActivityEditHistoryBinding

class EditHistoryListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditHistoryBinding
    private val viewModel: EditHistoryListModelView by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, EditHistoryListActivity::class.java)
        }
    }
}

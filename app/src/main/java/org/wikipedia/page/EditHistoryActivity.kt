package org.wikipedia.page

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.wikipedia.databinding.ActivityEditHistoryBinding

class EditHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, EditHistoryActivity::class.java)
        }
    }
}

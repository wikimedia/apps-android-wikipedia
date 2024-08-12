package org.wikipedia.games.onthisday

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityTalkTopicBinding
import org.wikipedia.util.Resource

class OnThisDayGameActivity : BaseActivity() {
    private lateinit var binding: ActivityTalkTopicBinding
    private val viewModel: OnThisDayGameViewModel by viewModels { OnThisDayGameViewModel.Factory(intent.extras!!) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTalkTopicBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""


        binding.talkRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                supportActionBar?.setDisplayShowTitleEnabled(binding.talkRecyclerView.computeVerticalScrollOffset() > (recyclerView.getChildAt(0).height / 2))
            }
        })

        viewModel.gameState.observe(this) {
            when (it) {
                is Resource.Success -> updateOnSuccess(it.data)
                is Resource.Error -> updateOnError(it.throwable)
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //menuInflater.inflate(R.menu..., menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.about_wmf -> {
                // TODO
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateOnSuccess(gameState: OnThisDayGameViewModel.GameState) {

    }

    private fun updateOnError(t: Throwable) {

    }

    companion object {

        fun newIntent(context: Context, invokeSource: Constants.InvokeSource): Intent {
            return Intent(context, OnThisDayGameActivity::class.java)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}

package org.wikipedia.feed.wikigames

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.compose.components.PageIndicator
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.databinding.ViewWikiGamesCardBinding
import org.wikipedia.extensions.getString
import org.wikipedia.feed.view.CardFooterView
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.games.onthisday.OnThisDayGameActivity

class WikiGamesCardView(context: Context) : DefaultFeedCardView<WikiGamesCard>(context), CardFooterView.Callback {
    interface Callback {
        fun onWikiGamesCardFooterClicked()
    }

    private val binding = ViewWikiGamesCardBinding.inflate(LayoutInflater.from(context), this, true)

    override var card: WikiGamesCard? = null
        set(value) {
            field = value
            value?.let {
                val langCode = it.wikiSite.languageCode
                setHeader(langCode, it)
                setContent(it)
                setFooterText(langCode)
            }
            WikiGamesEvent.submit("impression", "game_feed")
        }

    override var callback: FeedAdapter.Callback? = null
        set(value) {
            field = value
            binding.viewWikiGamesCardHeader.setCallback(value)
        }

    override fun onFooterClicked() {
        callback?.onWikiGamesCardFooterClicked()
    }

    private fun setContent(card: WikiGamesCard) {
        binding.gamesComposeView.setContent {
            BaseTheme {
                WikiGamesCard(
                    modifier = Modifier
                        .fillMaxWidth(),
                    card = card,
                    onPlayClick = {
                        WikiGamesEvent.submit("enter_click", "game_feed")
                        (context as? Activity)?.startActivityForResult(OnThisDayGameActivity.newIntent(context, Constants.InvokeSource.FEED, card.wikiSite), 0)
                    }
                )
            }
        }
    }

    private fun setHeader(langCode: String, card: WikiGamesCard) {
        binding.viewWikiGamesCardHeader
            .setTitle(
                context.getString(
                    langCode,
                    R.string.on_this_day_game_feed_entry_card_heading
                )
            )
            .setLangCode(langCode)
            .setCard(card)
    }

    private fun setFooterText(langCode: String) {
        binding.gamesCardFooter.setFooterActionText(actionText = context.getString(langCode, R.string.on_this_day_game_more_label), null)
        binding.gamesCardFooter.callback = this
    }
}

@Composable
fun WikiGamesCard(
    card: WikiGamesCard,
    modifier: Modifier = Modifier,
    onPlayClick: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { card.games.size })
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier
        ) { page ->
            when (val game = card.games[page]) {
                is WikiGame.TestGame -> {
                    WikiCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 8.dp),
                        elevation = 2.dp
                    ) {
                        Text(
                            text = game.name,
                            color = WikipediaTheme.colors.primaryColor,
                            fontSize = 32.sp
                        )
                    }
                }

                is WikiGame.OnThisDayGame -> {
                    when (game.state) {
                        is OnThisDayCardGameState.Preview -> {
                            OnThisDayGameCardPreview(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                game = game.state,
                                onPlayClick = onPlayClick
                            )
                        }
                        is OnThisDayCardGameState.InProgress -> {
                            OnTHisDayCardProgress(
                                game = game.state,
                                onContinueClick = onPlayClick
                            )
                        }
                        is OnThisDayCardGameState.Completed -> {}
                    }
                }
            }
        }

        if (card.games.size > 1) {
            PageIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                pagerState = pagerState
            )
        }
    }
}

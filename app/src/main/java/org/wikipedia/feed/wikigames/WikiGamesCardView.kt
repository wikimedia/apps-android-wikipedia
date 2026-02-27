package org.wikipedia.feed.wikigames

import android.content.Context
import android.view.LayoutInflater
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.compose.components.PageIndicator
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.databinding.ViewWikiGamesCardBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.view.CardFooterView
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter

class WikiGamesCardView(context: Context) : DefaultFeedCardView<WikiGamesCard>(context), CardFooterView.Callback {
    interface Callback {
        fun onWikiGamesCardFooterClicked()
        fun onThisDayGameCountDownFinished()
        fun onThisDayGameArchiveButtonClicked(wikiSite: WikiSite)
        fun onThisDayGamePlayButonClicked(wikiSite: WikiSite)
        fun onThisDayGameReviewResultsButtonClicked(wikiSite: WikiSite)
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
                WikiGamesCardContent(
                    modifier = Modifier
                        .fillMaxWidth(),
                    card = card,
                    onThisDayGameAction = { action ->
                        when (action) {
                            OnThisDayGameAction.CountdownFinished -> callback?.onThisDayGameCountDownFinished()
                            OnThisDayGameAction.Play -> callback?.onThisDayGamePlayButonClicked(card.wikiSite)
                            OnThisDayGameAction.PlayArchive -> callback?.onThisDayGameArchiveButtonClicked(card.wikiSite)
                            OnThisDayGameAction.ReviewResults -> callback?.onThisDayGameReviewResultsButtonClicked(card.wikiSite)
                        }
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
                    R.string.games_hub_activity_title
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
fun WikiGamesCardContent(
    card: WikiGamesCard,
    modifier: Modifier = Modifier,
    onThisDayGameAction: (OnThisDayGameAction) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { card.games.size })
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier
        ) { page ->
            when (val game = card.games[page]) {
                is WikiGame.OnThisDayGame -> {
                    when (game.state) {
                        is OnThisDayCardGameState.Preview -> {
                            OnThisDayGameCardPreview(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                state = game.state,
                                titleText = context.getString(game.state.langCode, R.string.on_this_day_game_title),
                                onPlayClick = { onThisDayGameAction(OnThisDayGameAction.Play) }
                            )
                        }
                        is OnThisDayCardGameState.InProgress -> {
                            OnThisDayCardProgress(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                state = game.state,
                                titleText = stringResource(R.string.on_this_day_game_title),
                                onContinueClick = { onThisDayGameAction(OnThisDayGameAction.Play) }
                            )
                        }
                        is OnThisDayCardGameState.Completed -> {
                            OnThisDayCardCompleted(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                state = game.state,
                                titleText = stringResource(R.string.on_this_day_game_title),
                                onReviewResult = { onThisDayGameAction(OnThisDayGameAction.ReviewResults) },
                                onPlayTheArchive = { onThisDayGameAction(OnThisDayGameAction.PlayArchive) },
                                onCountDownFinished = { onThisDayGameAction(OnThisDayGameAction.CountdownFinished) }
                            )
                        }
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

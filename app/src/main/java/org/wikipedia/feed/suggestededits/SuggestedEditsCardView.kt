package org.wikipedia.feed.suggestededits

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.view_suggested_edits_card.view.*
import org.wikipedia.R
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.view.CardFooterView
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter

class SuggestedEditsCardView(context: Context) : DefaultFeedCardView<SuggestedEditsCard>(context), SuggestedEditsFeedClient.Callback, CardFooterView.Callback {
    interface Callback {
        fun onSeCardClicked(card: SuggestedEditsCard, view: SeCardsViewHolder?)
        fun onSeCardFooterClicked()
    }

    private var card: SuggestedEditsCard? = null
    private var view: View = inflate(getContext(), R.layout.view_suggested_edits_card, this)

    override fun setCard(card: SuggestedEditsCard) {
        if (card == getCard()) {
            return
        }
        super.setCard(card)
        this.card = card
        header(card)
        updateContents()
    }

    override fun setCallback(callback: FeedAdapter.Callback?) {
        super.setCallback(callback)
        headerView.setCallback(callback)
    }

    private fun updateContents() {
        setUpSECardsRecyclerView()
        cardFooter.setFooterActionText(context.getString(R.string.suggested_card_more_edits))
        cardFooter.callback = this
    }

    private fun setUpSECardsRecyclerView() {
        seCardsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        seCardsRecyclerView.adapter = SECardsRecyclerViewAdapter()
    }

    internal inner class SECardsRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val seCardTypeList = ArrayList<DescriptionEditActivity.Action>()

        init {
            seCardTypeList.add(ADD_DESCRIPTION)
            seCardTypeList.add(ADD_CAPTION)
            seCardTypeList.add(ADD_IMAGE_TAGS)
        }

        override fun getItemCount(): Int {
            return seCardTypeList.size
        }

        override fun onCreateViewHolder(@NonNull parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
            return SeCardsViewHolder((parent.context as AppCompatActivity).layoutInflater
                    .inflate(R.layout.view_suggested_edits_card_item, parent, false), )
        }

        override fun onBindViewHolder(@NonNull holder: RecyclerView.ViewHolder, pos: Int) {
            (holder as SeCardsViewHolder).bindItem(card!!.age, seCardTypeList[pos])
            holder.suggestedEditsFragmentViewGroup.addOnClickListener({
                if (holder.itemClickable) {
                    holder.funnel.cardClicked(CardType.SUGGESTED_EDITS, if (holder.targetLanguage != null
                            && holder.targetLanguage.equals(holder.langFromCode)) holder.langFromCode else holder.targetLanguage)
                    if (callback != null) {
                        callback!!.onSeCardClicked(card!!, holder)
                    }
                }
            }, holder)
        }

        private fun Group.addOnClickListener(listener: View.OnClickListener, @NonNull holder: RecyclerView.ViewHolder) {
            referencedIds.forEach { id ->
                holder.itemView.findViewById<View>(id).setOnClickListener(listener)
            }
        }
    }

    private fun header(card: SuggestedEditsCard) {
        headerView!!.setTitle(card.title())
                .setLangCode("")
                .setCard(card)
                .setCallback(callback)
    }

    override fun updateCardContent(card: SuggestedEditsCard) {
        setCard(card)
    }

    override fun onFooterClicked() {
        if (callback != null) {
            callback!!.onSeCardFooterClicked()
        }
    }
}

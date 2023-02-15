package org.wikipedia.readinglist

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.databinding.ItemReadingListPreviewSaveSelectItemBinding
import org.wikipedia.databinding.ViewReadingListPreviewSaveDialogBinding
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.StringUtil
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.ViewUtil

class ReadingListPreviewSaveDialogView : FrameLayout {

    private val binding = ViewReadingListPreviewSaveDialogBinding.inflate(LayoutInflater.from(context), this, true)

    private lateinit var readingList: ReadingList
    private lateinit var savedReadingListPages: MutableList<ReadingListPage>

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(DrawableItemDecoration(context, R.attr.list_separator_drawable, drawStart = true, drawEnd = true, skipSearchBar = true))
    }

    fun setContentType(readingList: ReadingList, savedReadingListPages: MutableList<ReadingListPage>) {
        this.readingList = readingList
        this.savedReadingListPages = savedReadingListPages
        binding.recyclerView.adapter = ReadingListItemAdapter()
    }

    private inner class ReadingListItemHolder constructor(val itemBinding: ItemReadingListPreviewSaveSelectItemBinding) : DefaultViewHolder<View>(itemBinding.root), OnClickListener {
        private lateinit var readingListPage: ReadingListPage

        fun bindItem(readingListPage: ReadingListPage) {
            this.readingListPage = readingListPage
            itemBinding.container.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            itemBinding.articleName.text = StringUtil.fromHtml(readingListPage.displayTitle)
            ViewUtil.loadImage(itemBinding.articleThumbnail, readingListPage.thumbUrl, true)
            itemBinding.container.setOnClickListener(this)
            itemBinding.checkbox.setOnClickListener(this)
            updateState()
        }

        override fun onClick(v: View) {
            if (savedReadingListPages.contains(readingListPage)) {
                savedReadingListPages.remove(readingListPage)
            } else {
                savedReadingListPages.add(readingListPage)
            }
            updateState()
        }

        private fun updateState() {
            itemBinding.checkbox.isChecked = savedReadingListPages.contains(readingListPage)
        }
    }

    private inner class ReadingListItemAdapter : RecyclerView.Adapter<ReadingListItemHolder>() {
        override fun getItemCount(): Int {
            return readingList.pages.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): ReadingListItemHolder {
            return ReadingListItemHolder(ItemReadingListPreviewSaveSelectItemBinding.inflate(LayoutInflater.from(context)))
        }

        override fun onBindViewHolder(holder: ReadingListItemHolder, pos: Int) {
            holder.bindItem(readingList.pages[pos])
        }
    }
}

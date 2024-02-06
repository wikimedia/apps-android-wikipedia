package org.wikipedia.readinglist

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.ItemReadingListPreviewSaveSelectItemBinding
import org.wikipedia.databinding.ViewReadingListPreviewSaveDialogBinding
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.DateUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.ViewUtil
import java.util.Date

class ReadingListPreviewSaveDialogView : FrameLayout {

    interface Callback {
        fun onError()
        fun onSuccess(listTitle: String)
    }

    private val binding = ViewReadingListPreviewSaveDialogBinding.inflate(LayoutInflater.from(context), this, true)

    private lateinit var readingList: ReadingList
    private lateinit var savedReadingListPages: MutableList<ReadingListPage>
    private lateinit var callback: Callback
    private var currentReadingLists: MutableList<ReadingList>

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(DrawableItemDecoration(context, R.attr.list_divider, drawStart = true, drawEnd = true, skipSearchBar = true))
        currentReadingLists = AppDatabase.instance.readingListDao().getAllLists().toMutableList()
        binding.readingListTitle.doOnTextChanged { _, _, _, _ ->
            validateTitleAndList()
        }
    }

    fun setContentType(readingList: ReadingList, savedReadingListPages: MutableList<ReadingListPage>, callback: Callback) {
        this.readingList = readingList
        this.savedReadingListPages = savedReadingListPages
        this.callback = callback
        val defaultListTitle = context.getString(R.string.reading_lists_preview_header_title).plus(" " + DateUtil.getShortDayWithTimeString(Date()))
        binding.readingListTitleLayout.editText?.setText(defaultListTitle)
        validateTitleAndList()
        binding.recyclerView.adapter = ReadingListItemAdapter()
    }

    private fun validateTitleAndList() {
        val listTitle = binding.readingListTitle.text.toString()
        if (currentReadingLists.any { it.title == listTitle }) {
            binding.readingListTitleLayout.error =
                context.getString(R.string.reading_list_title_exists, listTitle)
            callback.onError()
        } else if (listTitle.isEmpty()) {
            binding.readingListTitleLayout.error = null
            callback.onError()
        } else if (savedReadingListPages.isEmpty()) {
            binding.readingListTitleLayout.error = null
            callback.onError()
        } else {
            binding.readingListTitleLayout.error = null
            callback.onSuccess(listTitle)
        }
    }

    private inner class ReadingListItemHolder constructor(val itemBinding: ItemReadingListPreviewSaveSelectItemBinding) : DefaultViewHolder<View>(itemBinding.root), OnClickListener {
        private lateinit var readingListPage: ReadingListPage

        fun bindItem(readingListPage: ReadingListPage) {
            this.readingListPage = readingListPage
            itemBinding.container.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            itemBinding.articleName.text = StringUtil.fromHtml(readingListPage.displayTitle)
            itemBinding.articleDescription.isVisible = !readingListPage.description.isNullOrEmpty()
            itemBinding.articleDescription.text = StringUtil.fromHtml(readingListPage.description)
            ViewUtil.loadImage(itemBinding.articleThumbnail, readingListPage.thumbUrl, roundedCorners = true)
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
            validateTitleAndList()
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

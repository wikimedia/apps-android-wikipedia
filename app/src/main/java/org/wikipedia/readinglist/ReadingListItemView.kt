package org.wikipedia.readinglist

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.os.FileObserver
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ItemReadingListBinding
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.*
import org.wikipedia.views.ViewUtil
import java.io.File
import java.io.FileOutputStream

class ReadingListItemView : ConstraintLayout, BaseActivity.Callback {
    interface Callback {
        fun onClick(readingList: ReadingList)
        fun onRename(readingList: ReadingList)
        fun onDelete(readingList: ReadingList)
        fun onSaveAllOffline(readingList: ReadingList)
        fun onRemoveAllOffline(readingList: ReadingList)
    }

    enum class Description {
        DETAIL, SUMMARY
    }

    private val binding = ItemReadingListBinding.inflate(LayoutInflater.from(context), this)
    private var readingList: ReadingList? = null
    private val imageViews = listOf(binding.itemImage1, binding.itemImage2, binding.itemImage3, binding.itemImage4)
    private val activity:BaseActivity = context as BaseActivity
    var callback: Callback? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setPadding(0, DimenUtil.roundedDpToPx(16f), 0, DimenUtil.roundedDpToPx(16f))
        setBackgroundResource(ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackground))
        isClickable = true
        isFocusable = true
        clearThumbnails()
        DeviceUtil.setContextClickAsLongClick(this)
        activity.callback = this

        setOnClickListener {
            readingList?.let {
                callback?.onClick(it)
            }
        }

        setOnLongClickListener { view ->
            readingList?.let {
                PopupMenu(context, view, Gravity.END).let { menu ->
                    menu.menuInflater.inflate(R.menu.menu_reading_list_item, menu.menu)
                    if (it.isDefault) {
                        menu.menu.findItem(R.id.menu_reading_list_rename).isVisible = false
                        menu.menu.findItem(R.id.menu_reading_list_delete).isVisible = false
                    }
                    menu.setOnMenuItemClickListener(OverflowMenuClickListener(it))
                    menu.show()
                }
            }
            false
        }

        binding.itemOverflowMenu.setOnClickListener { anchorView ->
            readingList?.let {
                PopupMenu(context, anchorView, Gravity.END).let { menu ->
                    menu.menuInflater.inflate(R.menu.menu_reading_list_item, menu.menu)
                    if (it.isDefault) {
                        menu.menu.findItem(R.id.menu_reading_list_rename).isVisible = false
                        menu.menu.findItem(R.id.menu_reading_list_delete).isVisible = false
                    }
                    menu.setOnMenuItemClickListener(OverflowMenuClickListener(it))
                    menu.show()
                }
            }
        }
    }

    fun setReadingList(readingList: ReadingList, description: Description) {
        this.readingList = readingList
        val isDetailView = description == Description.DETAIL
        binding.itemDescription.maxLines = if (isDetailView) Int.MAX_VALUE else resources.getInteger(R.integer.reading_list_description_summary_view_max_lines)
        val text: CharSequence = if (isDetailView) buildStatisticalDetailText(readingList) else buildStatisticalSummaryText(readingList)
        binding.itemReadingListStatisticalDescription.text = text
        updateDetails()
        if (binding.itemImage1.visibility == VISIBLE) {
            updateThumbnails()
        }
    }

    fun setThumbnailVisible(visible: Boolean) {
        imageViews.forEach {
            it.visibility = if (visible) VISIBLE else GONE
        }
        binding.defaultListEmptyImage.visibility = if (visible) VISIBLE else GONE
    }

    fun setTitleTextAppearance(@StyleRes id: Int) {
        TextViewCompat.setTextAppearance(binding.itemTitle, id)
    }

    fun setSearchQuery(searchQuery: String?) {
        // highlight search term within the text
        StringUtil.boldenKeywordText(binding.itemTitle, binding.itemTitle.text.toString(), searchQuery)
    }

    fun setOverflowViewVisibility(visibility: Int) {
        binding.itemOverflowMenu.visibility = visibility
    }

    private fun updateDetails() {
        readingList?.let {
            binding.defaultListEmptyImage.visibility = if (it.isDefault && it.pages.size == 0 && binding.itemImage1.visibility == VISIBLE) VISIBLE else GONE
            binding.itemTitle.text = it.title
            if (it.isDefault) {
                binding.itemDescription.text = context.getString(R.string.default_reading_list_description)
                binding.itemDescription.visibility = VISIBLE
            } else {
                binding.itemDescription.text = it.description
                binding.itemDescription.visibility = if (it.description.isNullOrEmpty()) GONE else VISIBLE
            }
        }
    }

    private fun clearThumbnails() {
        imageViews.forEach {
            ViewUtil.loadImage(it, null)
        }
    }

    private fun updateThumbnails() {
        readingList?.let {
            clearThumbnails()
            val thumbUrls = it.pages.mapNotNull { page -> page.thumbUrl }
                .filterNot { url -> url.isEmpty() }
            (imageViews zip thumbUrls).forEach { (imageView, url) ->
                ViewUtil.loadImage(imageView, url)
            }
        }
    }

    private fun buildStatisticalSummaryText(readingList: ReadingList): String {
        return resources.getQuantityString(R.plurals.format_reading_list_statistical_summary,
                readingList.pages.size, readingList.pages.size, statsTextListSize(readingList))
    }

    private fun buildStatisticalDetailText(readingList: ReadingList): String {
        return resources.getQuantityString(R.plurals.format_reading_list_statistical_detail,
                readingList.pages.size, readingList.numPagesOffline, readingList.pages.size, statsTextListSize(readingList))
    }

    private fun statsTextListSize(readingList: ReadingList): Float {
        return readingList.sizeBytesFromPages / 1.coerceAtLeast(resources.getInteger(R.integer.reading_list_item_size_bytes_per_unit)).toFloat()
    }

    private inner class OverflowMenuClickListener constructor(private val list: ReadingList?) : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.menu_reading_list_rename -> {
                    list?.let { callback?.onRename(it) }
                    return true
                }
                R.id.menu_reading_list_delete -> {
                    list?.let { callback?.onDelete(it) }
                    return true
                }
                R.id.menu_reading_list_save_all_offline -> {
                    list?.let { callback?.onSaveAllOffline(it) }
                    return true
                }
                R.id.menu_reading_list_remove_all_offline -> {
                    list?.let { callback?.onRemoveAllOffline(it) }
                    return true
                }
                R.id.menu_reading_list_export -> {
                    handlePermissionsAndExport()
                    return true
                }
                else -> return false
            }
        }
    }

    private fun handlePermissionsAndExport() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            exportListAsCsv()
        } else {
            activity.requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    lateinit var observer: FileObserver
    private fun exportListAsCsv() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return
        }
        val appExportsDir =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "wikipedia_exported_files")
        if (!appExportsDir.exists()) {
            appExportsDir.mkdirs()
        }
        val csvFile = File(appExportsDir.path + "/" + readingList?.listTitle + ".csv")
       observer = object :FileObserver(appExportsDir, ALL_EVENTS) {

           override fun onEvent(event: Int, file: String?) {
               if(event== CREATE) {

               }
           }
       }
       observer.startWatching();
        FileOutputStream(csvFile, true).bufferedWriter().use {
            it.write("pageTitle, language ,link")
            it.newLine()
        }
        readingList?.let {
            it.pages.forEach { page ->
                val pageTitle = ReadingListPage.toPageTitle(page)
                val uri = pageTitle.uri
                val language = pageTitle.wikiSite.languageCode
                FileOutputStream(csvFile, true).bufferedWriter().use { writer ->
                    writer.write(pageTitle.prefixedText + "," + language + "," + uri)
                    writer.newLine()
                }
            }
        }
    }

    override fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
           exportListAsCsv()
        } else {
            FeedbackUtil.showMessage(activity, R.string.reading_list_export_write_permission_rationale)
        }
    }
}

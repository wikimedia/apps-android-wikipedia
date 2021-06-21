package org.wikipedia.gallery

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil.loadImage

class GalleryThumbnailScrollView constructor(context: Context, attrs: AttributeSet? = null) : RecyclerView(context, attrs) {

    fun interface GalleryViewListener {
        fun onGalleryItemClicked(view: ImageView, thumbUrl: String, imageName: String)
    }

    private val pressAnimation = AnimationUtils.loadAnimation(context, R.anim.thumbnail_item_press)
    private val releaseAnimation = AnimationUtils.loadAnimation(context, R.anim.thumbnail_item_release)
    var listener: GalleryViewListener? = null

    init {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    fun setGalleryList(list: List<MwQueryPage>) {
        adapter = GalleryViewAdapter(list)
    }

    @SuppressLint("ClickableViewAccessibility")
    private inner class GalleryItemHolder constructor(itemView: View) : ViewHolder(itemView), OnClickListener, OnTouchListener {
        private lateinit var galleryItem: MwQueryPage
        private val imageView = itemView.findViewById<ImageView>(R.id.gallery_thumbnail_image)

        fun bindItem(item: MwQueryPage) {
            galleryItem = item
            galleryItem.imageInfo()?.let {
                imageView.isFocusable = true
                imageView.setOnClickListener(this)
                imageView.setOnTouchListener(this)
                loadImage(imageView, it.thumbUrl)
            }
        }

        override fun onClick(v: View) {
            galleryItem.imageInfo()?.let {
                listener?.onGalleryItemClicked(v as ImageView, it.thumbUrl, StringUtil.addUnderscores(galleryItem.title()))
            }
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.startAnimation(pressAnimation)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.startAnimation(releaseAnimation)
            }
            return false
        }
    }

    private inner class GalleryViewAdapter constructor(private val list: List<MwQueryPage>) : Adapter<GalleryItemHolder>() {
        override fun getItemCount(): Int {
            return list.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): GalleryItemHolder {
            return GalleryItemHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gallery_thumbnail, parent, false))
        }

        override fun onBindViewHolder(holder: GalleryItemHolder, pos: Int) {
            holder.bindItem(list[pos])
        }
    }
}

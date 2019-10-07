package org.wikipedia.suggestededits

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_suggested_edits_task_item.view.*
import org.wikipedia.Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

internal class SuggestedEditsTaskView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_suggested_edits_task_item, this)
        taskImageDetailView.setImageParams(resources.getDimension(R.dimen.suggested_edits_task_icon_size).toInt(), resources.getDimension(R.dimen.suggested_edits_task_icon_size).toInt())
        taskImageDetailView.setImageBackgroundParams(resources.getDimension(R.dimen.suggested_edits_task_icon_background_size).toInt(), resources.getDimension(R.dimen.suggested_edits_task_icon_background_size).toInt())
        taskImageDetailView.setCaseForTitle(true)
        taskImageDetailView.setTitleTextSize(if (DeviceUtil.isDeviceTablet()) IMAGE_DETAIL_TEXT_SIZE_TABLET else IMAGE_DETAIL_TEXT_SIZE_PHONE)
    }

    private fun updateTranslateActionUI() {
        suggetedEditsTranslateImage.imageTintList = ColorStateList.valueOf(ResourceUtil.getThemedColor(context, if (WikipediaApp.getInstance().language().appLanguageCodes.size >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION)
            R.attr.colorAccent else R.attr.material_theme_de_emphasised_color))
        suggetedEditsTranslateActionText.setTextColor(ResourceUtil.getThemedColor(context, if (WikipediaApp.getInstance().language().appLanguageCodes.size >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION)
            R.attr.colorAccent else R.attr.material_theme_de_emphasised_color))
    }

    fun setUpViews(task: SuggestedEditsTask, callback: Callback?) {
        setResourcesByDeviceSize()
        updateTranslateActionUI()
        taskImageDetailView.setTitle(task.title!!)
        taskImageDetailView.setDescription(task.description!!)
        taskImageDetailView.setImageDrawable(task.imageDrawable!!)
        taskImageDetailView.setImageTint(ResourceUtil.getThemedAttributeId(context!!, R.attr.themed_icon_color))

        addContainer.setOnClickListener {
            if (!task.disabled) {
                callback?.onViewClick(task, false)
            }
        }
        translateContainer.setOnClickListener {
            if (!task.disabled) {
                callback?.onViewClick(task, true)
            }
        }
    }

    private fun setResourcesByDeviceSize() {
        if (DeviceUtil.isDeviceTablet()) {
            val params: LayoutParams = taskInfoContainer.layoutParams as LayoutParams
            params.setMargins(0, DimenUtil.roundedDpToPx(TASK_CONTAINER_TABLET_TOP_BOTTOM_MARGIN), 0, DimenUtil.roundedDpToPx(TASK_CONTAINER_TABLET_TOP_BOTTOM_MARGIN))
            taskInfoContainer.layoutParams = params
            actionLayout.gravity = CENTER
            val actionLayoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            actionLayoutParams.setMargins(0, 0, 0, 0)
            actionLayout.layoutParams = actionLayoutParams
            taskImageDetailView.setUpViewForTablet()

        }
    }

    interface Callback {
        fun onViewClick(task: SuggestedEditsTask, isTranslate: Boolean)
    }

    companion object {
        val IMAGE_DETAIL_TEXT_SIZE_PHONE = 14f
        val IMAGE_DETAIL_TEXT_SIZE_TABLET = 20f
        val TASK_CONTAINER_TABLET_TOP_BOTTOM_MARGIN = 36f
    }
}

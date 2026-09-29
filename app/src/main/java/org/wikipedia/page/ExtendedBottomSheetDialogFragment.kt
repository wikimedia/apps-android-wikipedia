package org.wikipedia.page

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.annotation.StyleRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wikipedia.R
import org.wikipedia.analytics.BreadcrumbsContextHelper
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.ResourceUtil

open class ExtendedBottomSheetDialogFragment(
    private val startExpanded: Boolean = false
) : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BreadCrumbLogEvent.logScreenShown(requireContext(), this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return ExtendedBottomSheetDialog(requireContext(), theme)
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            it.window?.let { window ->
                DeviceUtil.setNavigationBarColor(window, ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
            }
            if (startExpanded) {
                it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                    BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }

    class ExtendedBottomSheetDialog(context: Context, @StyleRes theme: Int) :
        BottomSheetDialog(context, theme) {

        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            window?.let {
                BreadcrumbsContextHelper.dispatchTouchEvent(it, ev)
            }
            return super.dispatchTouchEvent(ev)
        }
    }
}

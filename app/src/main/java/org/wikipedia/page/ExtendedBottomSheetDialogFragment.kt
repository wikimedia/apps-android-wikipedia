package org.wikipedia.page

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.annotation.StyleRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wikipedia.R
import org.wikipedia.analytics.BreadcrumbsContextHelper
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

open class ExtendedBottomSheetDialogFragment(
    private val startExpanded: Boolean = false,
    private val keepBelowStatusBar: Boolean = false
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
            it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                if (keepBelowStatusBar) {
                    limitMaxHeightBelowStatusBar(sheet, behavior)
                }
                if (startExpanded) {
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }

    private fun limitMaxHeightBelowStatusBar(sheet: View, behavior: BottomSheetBehavior<View>) {
        (sheet.parent as? View)?.doOnLayout { container ->
            val statusBarHeight = ViewCompat.getRootWindowInsets(container)
                ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            behavior.maxHeight = container.height - statusBarHeight - DimenUtil.roundedDpToPx(EXPANDED_TOP_GAP_DP)
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

    companion object {
        private const val EXPANDED_TOP_GAP_DP = 16f
    }
}

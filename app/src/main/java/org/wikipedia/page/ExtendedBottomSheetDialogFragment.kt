package org.wikipedia.page

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.annotation.StyleRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        dialog?.window?.let { window ->
            DeviceUtil.setNavigationBarColor(window, ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
        }
        bottomSheet?.let { sheet ->
            if (keepBelowStatusBar) {
                (sheet.parent as? View)?.let { container ->
                    container.removeOnLayoutChangeListener(sheetContainerLayoutChangeListener)
                    container.addOnLayoutChangeListener(sheetContainerLayoutChangeListener)
                }
            }
            if (startExpanded) {
                BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private val bottomSheet: View?
        get() = dialog?.findViewById(com.google.android.material.R.id.design_bottom_sheet)

    private val sheetContainerLayoutChangeListener = View.OnLayoutChangeListener { container, _, top, _, bottom, _, oldTop, _, oldBottom ->
        if (bottom - top != oldBottom - oldTop) {
            bottomSheet?.let { sheet ->
                val statusBarHeight = ViewCompat.getRootWindowInsets(container)
                    ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
                BottomSheetBehavior.from(sheet).maxHeight = container.height - statusBarHeight - DimenUtil.roundedDpToPx(EXPANDED_TOP_GAP_DP)
                sheet.requestLayout()
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

    companion object {
        private const val EXPANDED_TOP_GAP_DP = 16f
    }
}

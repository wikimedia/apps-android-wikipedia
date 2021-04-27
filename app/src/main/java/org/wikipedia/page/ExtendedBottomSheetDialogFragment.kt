package org.wikipedia.page

import android.os.Build
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wikipedia.WikipediaApp

open class ExtendedBottomSheetDialogFragment : BottomSheetDialogFragment() {

    protected fun disableBackgroundDim() {
        requireDialog().window?.setDimAmount(0f)
    }

    protected fun setNavigationBarColor(@ColorInt color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isDarkThemeOrDarkBackground = (WikipediaApp.getInstance().currentTheme.isDark ||
                    color == ContextCompat.getColor(requireContext(), android.R.color.black))

            requireDialog().window?.run {
                navigationBarColor = color
                decorView.systemUiVisibility = if (isDarkThemeOrDarkBackground) decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                else View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or decorView.systemUiVisibility
            }
        }
    }
}

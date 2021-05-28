package org.wikipedia.page

import android.os.Build
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wikipedia.WikipediaApp
import org.wikipedia.ktx.insetsControllerCompat

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
                insetsControllerCompat?.isAppearanceLightNavigationBars = !isDarkThemeOrDarkBackground
            }
        }
    }
}

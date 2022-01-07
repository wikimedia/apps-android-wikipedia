package org.wikipedia.theme

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.wikipedia.Constants
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.page.ExclusiveBottomSheetPresenter

class ThemeFittingRoomActivity : SingleFragmentActivity<ThemeFittingRoomFragment>(), ThemeChooserDialog.Callback {
    private var themeChooserDialog: ThemeChooserDialog? = null
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            themeChooserDialog = ThemeChooserDialog.newInstance(Constants.InvokeSource.SETTINGS)
            bottomSheetPresenter.show(supportFragmentManager, themeChooserDialog!!)
        }

        // Don't let changed theme affects the status bar color and navigation bar color
        setStatusBarColor(ContextCompat.getColor(this, android.R.color.black))
        setNavigationBarColor(ContextCompat.getColor(this, android.R.color.black))
    }

    override fun createFragment(): ThemeFittingRoomFragment {
        return ThemeFittingRoomFragment.newInstance()
    }

    override fun onToggleDimImages() {
        ActivityCompat.recreate(this)
    }

    override fun onToggleReadingFocusMode() {
    }

    override fun onCancelThemeChooser() {
        finish()
    }

    companion object {
        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, ThemeFittingRoomActivity::class.java)
        }
    }
}

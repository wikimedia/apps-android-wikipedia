package androidx.drawerlayout.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View

class FixedDrawerLayout(context: Context, attrs: AttributeSet? = null) : DrawerLayout(context, attrs) {

    fun setSlidingEnabled(enable: Boolean) {
        if (enable) {
            this.setDrawerLockMode(LOCK_MODE_UNLOCKED)
        } else {
            this.setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED)
        }
    }

    // TODO: Remove this class when Google updates the Support library.
    // This solves an intermittent crash when using DrawerLayout.
    // https://issuetracker.google.com/issues/37007884
    override fun isContentView(child: View?): Boolean {
        if (child == null) {
            return false
        }
        return super.isContentView(child)
    }
}

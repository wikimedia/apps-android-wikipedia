package org.wikipedia.extensions

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle

val Activity.isStarted get(): Boolean {
    return ((this as AppCompatActivity).lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
}

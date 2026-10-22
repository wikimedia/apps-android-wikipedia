package org.wikipedia.performance

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.metrics.performance.PerformanceMetricsState

@Composable
fun rememberMetricsStateHolder(): PerformanceMetricsState.Holder {
    val view = LocalView.current
    return remember(view) { PerformanceMetricsState.getHolderForHierarchy(view) }
}

/**
 * Attaches the state [key]=[value] to every frame rendered while [value] returns non-null and the
 * hosting lifecycle is resumed. This is a no-op unless a JankStats instance is tracking the window.
 *
 * [value] is observed through a snapshot flow instead of being read during composition, so changes
 * in the value don't cause the caller to recompose.
 */
@Composable
fun TrackJankState(key: String, value: () -> String?) {
    val holder = rememberMetricsStateHolder()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentValue by rememberUpdatedState(value)

    LaunchedEffect(holder, lifecycleOwner, key) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Only remove the state if this call put it, so that another composable using the
            // same key isn't affected.
            var isStatePut = false
            try {
                snapshotFlow { currentValue() }.collect {
                    val metricsState = holder.state ?: return@collect
                    if (it != null) {
                        metricsState.putState(key, it)
                        isStatePut = true
                    } else if (isStatePut) {
                        metricsState.removeState(key)
                        isStatePut = false
                    }
                }
            } finally {
                if (isStatePut) {
                    holder.state?.removeState(key)
                }
            }
        }
    }
}

/**
 * Attaches the state [key]=[scrollingValue] to every frame rendered while [scrollableState] is
 * being dragged or flung.
 */
@Composable
fun TrackScrollJank(scrollableState: ScrollableState, key: String, scrollingValue: String = "Scrolling") {
    TrackJankState(key) { if (scrollableState.isScrollInProgress) scrollingValue else null }
}

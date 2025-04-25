package org.wikipedia.yearinreview

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class YearInReviewViewModelFactory(private var appContext: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YearInReviewViewModel::class.java)) {
            return YearInReviewViewModel(appContext) as T
        }
        throw IllegalArgumentException("ViewModel Instantiation Error")
    }
}

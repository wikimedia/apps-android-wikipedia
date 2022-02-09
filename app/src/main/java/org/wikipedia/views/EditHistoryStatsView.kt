package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import org.wikipedia.databinding.ViewEditHistoryStatsBinding

class EditHistoryStatsView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    val binding = ViewEditHistoryStatsBinding.inflate(LayoutInflater.from(context), this)
}

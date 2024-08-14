package org.wikipedia.games.onthisday

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import org.wikipedia.R
import org.wikipedia.databinding.ViewGameWeeklyActivityBinding
import org.wikipedia.util.ResourceUtil
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WeeklyActivityView(context: Context) : LinearLayout(context) {
    private val binding = ViewGameWeeklyActivityBinding.inflate(LayoutInflater.from(context), this)
    private val imageViews = listOf(binding.day1image, binding.day2image, binding.day3image, binding.day4image, binding.day5image, binding.day6image, binding.day7image)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        orientation = VERTICAL
    }

    fun setWeekStats(startDate: LocalDate, gameState: OnThisDayGameViewModel.GameState) {
        val endDate = startDate.plusDays(6)

        binding.weekText.text = context.getString(R.string.on_this_day_game_week_from_to, startDate.format(DateTimeFormatter.ofPattern("MMMM d")),
            if (startDate.monthValue == endDate.monthValue) endDate.format(DateTimeFormatter.ofPattern("d")) else endDate.format(DateTimeFormatter.ofPattern("MMMM d")))

        val tintSuccess = ResourceUtil.getThemedColorStateList(context, R.attr.success_color)
        val tintInactive = ResourceUtil.getThemedColorStateList(context, R.attr.inactive_color)

        imageViews.forEachIndexed { index, imageView ->
            val date = startDate.plusDays(index.toLong())

            if (date.isBefore(OnThisDayGameViewModel.gameStartDate) || date.isAfter(OnThisDayGameViewModel.gameEndDate)) {
                imageView.setImageResource(R.drawable.ic_horizontal_rule_24)
                imageView.imageTintList = tintInactive
            } else {
                gameState.answerStateHistory[date.year]?.get(date.monthValue)?.get(date.dayOfMonth)?.let { answerState ->
                    imageView.setImageResource(R.drawable.ic_star_24)
                    imageView.imageTintList = tintSuccess
                } ?: run {
                    imageView.setImageResource(R.drawable.ic_circle_outline_24)
                    imageView.imageTintList = tintInactive
                }
            }
        }
    }
}

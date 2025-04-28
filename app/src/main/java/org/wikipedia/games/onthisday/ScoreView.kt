package org.wikipedia.games.onthisday

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import org.wikipedia.R
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

class ScoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val scoreViews = mutableListOf<ShapeableImageView>()

    init {
        orientation = HORIZONTAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun generateViews(size: Int) {
        removeAllViews()
        scoreViews.clear()

        repeat(size) {
            val scoreView = createImageView()
            scoreViews.add(scoreView)
            addView(scoreView)
        }
    }

    fun updateScore(answerState: List<Boolean>, currentQuestionIndex: Int, gotToNext: Boolean) {
        if (currentQuestionIndex >= answerState.size) {
            return
        }

        val answer = answerState[currentQuestionIndex]
        val scoreViewToUpdate = scoreViews[currentQuestionIndex]
        // current question, user has not submitted answer
        if (!gotToNext) {
            scoreViewToUpdate.setImageResource(R.drawable.shape_circle)
            scoreViewToUpdate.imageTintList = ColorStateList.valueOf(
                ResourceUtil.getThemedColor(context, R.attr.paper_color)
            )
            return
        }
        // user submitted answer
        if (answer) {
            scoreViewToUpdate.setImageResource(R.drawable.checked)
            scoreViewToUpdate.imageTintList = ColorStateList.valueOf(
                ResourceUtil.getThemedColor(context, R.attr.success_color)
            )
        } else {
            scoreViewToUpdate.setImageResource(R.drawable.ic_cancel_24px)
            scoreViewToUpdate.imageTintList = ColorStateList.valueOf(
                ResourceUtil.getThemedColor(context, R.attr.destructive_color)
            )
        }
    }

    fun updateInitialScores(answerState: List<Boolean>, currentQuestionIndex: Int) {
        for (i in answerState.indices) {
            val scoreViewToUpdate = scoreViews[i]
            when {
                // future questions
                i >= currentQuestionIndex -> {
                    scoreViewToUpdate.imageTintList = ColorStateList.valueOf(
                        ResourceUtil.getThemedColor(context, R.attr.paper_color)
                    )
                }

                // questions that have already been answered
                i < currentQuestionIndex -> {
                    if (answerState[i]) {
                        scoreViewToUpdate.setImageResource(R.drawable.ic_check_circle_black_24dp)
                        scoreViewToUpdate.imageTintList = ColorStateList.valueOf(
                            ResourceUtil.getThemedColor(context, R.attr.success_color)
                        )
                    } else {
                        scoreViewToUpdate.setImageResource(R.drawable.ic_cancel_24px)
                        scoreViewToUpdate.imageTintList = ColorStateList.valueOf(
                            ResourceUtil.getThemedColor(context, R.attr.destructive_color)
                        )
                    }
                }
            }
        }
    }

    private fun createImageView(): ShapeableImageView {
        val imageView = ShapeableImageView(context)

        val size = DimenUtil.dpToPx(22f).toInt()
        val marginSize = DimenUtil.dpToPx(2f).toInt()
        val layoutParams = LayoutParams(size, size)
        val paddingSize = 2
        layoutParams.marginStart = marginSize
        layoutParams.marginEnd = marginSize

        imageView.layoutParams = layoutParams
        imageView.setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        imageView.strokeWidth = 1f
        imageView.strokeColor = ColorStateList.valueOf(
            ResourceUtil.getThemedColor(context, R.attr.paper_color)
        )
        imageView.setPadding(paddingSize, paddingSize, paddingSize, paddingSize)
        imageView.shapeAppearanceModel = ShapeAppearanceModel.builder(
            context,
            0,
            R.style.CircularImageView
        ).build()
        return imageView
    }
}

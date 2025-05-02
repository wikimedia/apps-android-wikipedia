package org.wikipedia.games.onthisday

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
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
        updateScoreViewAppearance(
            scoreView = scoreViews[currentQuestionIndex],
            isCorrect = answerState[currentQuestionIndex],
            isAnswered = gotToNext // when false, user has not answered
        )
    }

    fun updateInitialScores(answerState: List<Boolean>, currentQuestionIndex: Int) {
        for (i in answerState.indices) {
            val isAnswered = i < currentQuestionIndex
            val isCorrect = isAnswered && answerState[i]
            updateScoreViewAppearance(scoreViews[i], isCorrect, isAnswered)
        }
    }

    private fun updateScoreViewAppearance(
        scoreView: ImageView,
        isCorrect: Boolean,
        isAnswered: Boolean
    ) {
        if (!isAnswered) {
            scoreView.setImageResource(R.drawable.shape_circle)
            scoreView.imageTintList = ColorStateList.valueOf(
                ResourceUtil.getThemedColor(context, R.attr.paper_color)
            )
            return
        }

        if (isCorrect) {
            scoreView.setImageResource(R.drawable.ic_check_circle_black_24dp)
            scoreView.imageTintList = ColorStateList.valueOf(
                ResourceUtil.getThemedColor(context, R.attr.success_color)
            )
        } else {
            scoreView.setImageResource(R.drawable.ic_cancel_24px)
            scoreView.imageTintList = ColorStateList.valueOf(
                ResourceUtil.getThemedColor(context, R.attr.destructive_color)
            )
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

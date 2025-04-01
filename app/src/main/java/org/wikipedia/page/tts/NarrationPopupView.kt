package org.wikipedia.page.tts

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.core.view.doOnDetach
import androidx.core.view.isVisible
import androidx.core.widget.PopupWindowCompat
import androidx.media3.session.SessionCommand
import org.wikipedia.R
import org.wikipedia.databinding.ViewNarrationPopupBinding
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil
import androidx.core.graphics.drawable.toDrawable

class NarrationPopupView(context: Context) : FrameLayout(context) {

    private var binding = ViewNarrationPopupBinding.inflate(LayoutInflater.from(context), this, true)
    private var popupWindowHost: PopupWindow? = null

    fun show(anchorView: View, pageTitle: PageTitle) {
        popupWindowHost = PopupWindow(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindowHost?.let {
            it.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            PopupWindowCompat.setOverlapAnchor(it, true)
            it.showAsDropDown(anchorView, 0, anchorView.height - DimenUtil.roundedDpToPx(48f*4), Gravity.END or Gravity.BOTTOM)
        }

        anchorView.doOnDetach {
            dismissPopupWindowHost()
        }

        binding.articleTitle.text = StringUtil.fromHtml(pageTitle.displayText)
        if (pageTitle.thumbUrl.isNullOrEmpty()) {
            binding.articleThumbnail.isVisible = false
        } else {
            binding.articleThumbnail.isVisible = true
            ViewUtil.loadImage(binding.articleThumbnail, pageTitle.thumbUrl)
        }
        updateSpeedButtons()
        updatePlayPauseButton()

        binding.seekBackButton.setOnClickListener {
            Tts.mediaController?.sendCustomCommand(SessionCommand(PlaybackService.CUSTOM_COMMAND_REWIND_SEC, Bundle()), Bundle())
            updatePlayPauseButton()
        }

        binding.seekForwardButton.setOnClickListener {
            Tts.mediaController?.sendCustomCommand(SessionCommand(PlaybackService.CUSTOM_COMMAND_FORWARD_SEC, Bundle()), Bundle())
            updatePlayPauseButton()
        }

        binding.playPauseButton.setOnClickListener {
            Tts.mediaController?.let {
                if (it.isPlaying) {
                    it.pause()
                } else {
                    it.play()
                }
            }
            updatePlayPauseButton()
        }

        binding.decreaseSpeedButton.setOnClickListener {
            PlaybackService.speechRate -= 0.1f
            Tts.mediaController?.setPlaybackSpeed(PlaybackService.speechRate)
            updateSpeedButtons()
        }

        binding.increaseSpeedButton.setOnClickListener {
            PlaybackService.speechRate += 0.1f
            Tts.mediaController?.setPlaybackSpeed(PlaybackService.speechRate)
            updateSpeedButtons()
        }

        binding.stopButton.setOnClickListener {

            context.stopService(Intent(context, PlaybackService::class.java))
            Tts.cleanup()

            dismissPopupWindowHost()
        }
    }

    private fun dismissPopupWindowHost() {
        popupWindowHost?.let {
            it.dismiss()
            popupWindowHost = null
        }
    }

    private fun updatePlayPauseButton() {
        Tts.mediaController?.let {
            binding.playPauseButton.setImageResource(if (it.isPlaying) R.drawable.ic_pause_black_24dp else R.drawable.ic_play_arrow_black_24dp)
        }
    }

    private fun updateSpeedButtons() {
        binding.decreaseSpeedButton.isEnabled = PlaybackService.speechRate > 0.1f
        binding.increaseSpeedButton.isEnabled = PlaybackService.speechRate < 2.0f
        binding.speedText.text = String.format("%.1fx", PlaybackService.speechRate)
    }
}

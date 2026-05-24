package org.wikipedia.page.tts

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil
import androidx.core.net.toUri

object Tts {

    var utterances: List<String> = emptyList()
    var currentUtterance = 0

    var audioUrl: String = ""


    var currentPageTitle: PageTitle? = null
        set(value) {
            if (value == null) {
                field = null
            } else {
                val title = PageTitle(value.prefixedText, value.wikiSite)
                title.description = value.description
                title.thumbUrl = value.thumbUrl
                field = title
            }
        }

    var mediaController: MediaController? = null

    fun cleanup() {
        mediaController?.stop()
        mediaController?.release()
        mediaController = null
    }

    fun start(context: Context, pageTitle: PageTitle?, audioUrl: String, utterances: List<String>) {
        currentPageTitle = pageTitle
        this.utterances = utterances
        currentUtterance = 0

        this.audioUrl = audioUrl

        //if (mediaController?.isConnected == false) {
            cleanup()
        //}

        if (mediaController == null) {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture.addListener(
                {
                    mediaController = controllerFuture.get()
                    // playerView.setPlayer(controller)
                    speak(context, audioUrl)
                },
                MoreExecutors.directExecutor()
            )
        } else {
            speak(context, audioUrl)
        }
    }

    private fun speak(context: Context, audioUrl: String) {

        val mediaItemBuilder = MediaItem.Builder()
            .setMediaId("media-1")
            .setMediaMetadata(MediaMetadata.Builder()
                .setArtist(context.getString(R.string.app_name))
                .setTitle(StringUtil.fromHtml(currentPageTitle?.displayText.orEmpty()))
                .setArtworkUri(currentPageTitle?.thumbUrl.orEmpty().toUri())
                .build()
            )

        if (audioUrl.isNotBlank()) {
            mediaItemBuilder.setUri(audioUrl.toUri())
        } else {
            mediaItemBuilder.setUri(currentPageTitle!!.uri.toUri())
        }

        WikipediaApp.instance.mainThreadHandler.post {
            mediaController?.setMediaItem(mediaItemBuilder.build())
            mediaController?.prepare()
            mediaController?.play()
        }
    }
}
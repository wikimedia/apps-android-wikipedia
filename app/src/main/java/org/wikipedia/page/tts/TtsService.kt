package org.wikipedia.page.tts

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.OptIn
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.Commands
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.util.log.L
import java.util.Locale
import kotlin.math.max

class PlaybackService : MediaSessionService() {
    private var currentSession: MediaSession? = null

    var textToSpeech: TextToSpeech? = null

    private val customLayoutCommandButtons: List<CommandButton> =
        listOf(
            CommandButton.Builder()
                .setDisplayName("Rewind")
                .setEnabled(true)
                .setIconResId(R.drawable.ic_replay_10)
                .setSessionCommand(SessionCommand(CUSTOM_COMMAND_REWIND_SEC, Bundle.EMPTY))
                .build(),
            CommandButton.Builder()
                .setDisplayName("Forward")
                .setEnabled(true)
                .setIconResId(R.drawable.ic_forward_10)
                .setSessionCommand(SessionCommand(CUSTOM_COMMAND_FORWARD_SEC, Bundle.EMPTY))
                .build(),
        )

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        L.d(">>>> PlaybackService onCreate")
        super.onCreate()

        createSession()
    }

    @OptIn(UnstableApi::class)
    private fun createSession() {
        currentSession = MediaSession.Builder(this, createPlayer())
            .setCallback(object : MediaSession.Callback {

                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): ConnectionResult {
                    L.d(">>>> MediaSession onConnect")
                    isRunning = true

                    // Recreate the player upon connection to a new controller.
                    // (It needs to be a different type of player based on TTS vs audio URL)
                    // TODO: is this right?
                    currentSession?.player?.stop()
                    currentSession?.player?.release()
                    currentSession?.player = createPlayer()

                    if (session.isMediaNotificationController(controller)) {
                        val sessionCommands =
                            ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                                .also { builder ->
                                    customLayoutCommandButtons.forEach { commandButton ->
                                        commandButton.sessionCommand?.let { builder.add(it) }
                                    }
                                }
                                .build()
                        val playerCommands =
                            ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                                //.remove(COMMAND_SEEK_TO_PREVIOUS)
                                //.remove(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                                //.remove(COMMAND_SEEK_TO_NEXT)
                                //.remove(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                                .build()

                        // Custom layout and available commands to configure the legacy/framework session.
                        return ConnectionResult.AcceptedResultBuilder(session)
                            .setCustomLayout(customLayoutCommandButtons)
                            .setAvailablePlayerCommands(playerCommands)
                            .setAvailableSessionCommands(sessionCommands)
                            .build()
                    } else {
                        // Default commands with default custom layout for all other controllers.

                        val sessionCommands = ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                            .also { builder ->
                                customLayoutCommandButtons.forEach { commandButton ->
                                    commandButton.sessionCommand?.let { builder.add(it) }
                                }
                            }.build()
                        val playerCommands = ConnectionResult.DEFAULT_PLAYER_COMMANDS

                        return ConnectionResult.AcceptedResultBuilder(session)
                            .setAvailablePlayerCommands(playerCommands)
                            .setAvailableSessionCommands(sessionCommands)
                            .build()
                    }
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    L.d(">>>> MediaSession onCustomCommand: ${customCommand.customAction}")
                    if (customCommand.customAction == CUSTOM_COMMAND_REWIND_SEC) {
                        if (session.player is TtsPlayer) {
                            (session.player as TtsPlayer).speakPrevUtterance()
                        } else {
                            session.player.seekTo(session.player.currentPosition - 10_000)
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    } else if (customCommand.customAction == CUSTOM_COMMAND_FORWARD_SEC) {
                        if (session.player is TtsPlayer) {
                            (session.player as TtsPlayer).speakNextUtterance()
                        } else {
                            session.player.seekTo(session.player.currentPosition + 10_000)
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    return super.onCustomCommand(session, controller, customCommand, args)
                }

                override fun onDisconnected(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ) {
                    L.d(">>>> MediaSession onDisconnected")
                    isRunning = false
                    super.onDisconnected(session, controller)
                }

                override fun onPlaybackResumption(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    L.d(">>>> MediaSession onPlaybackResumption")
                    // TODO: Implement playback resumption, i.e. restore the playback state after
                    // the session was disconnected and reconnected.
                    return super.onPlaybackResumption(mediaSession, controller)
                }
            })
            .build()
    }

    override fun onDestroy() {
        L.d(">>>> PlaybackService onDestroy")
        currentSession?.run {
            player.stop()
            player.release()
            release()
            currentSession = null
        }
        isRunning = false
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        L.d(">>>> PlaybackService onTaskRemoved")
        val player = currentSession?.player
        if (player != null && (!player.playWhenReady
            || player.mediaItemCount == 0
            || player.playbackState == Player.STATE_ENDED)) {
            // Stop the service if not playing, continue playing in the background
            // otherwise.
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        L.d(">>>> PlaybackService onGetSession")
        return currentSession
    }

    private fun createPlayer(): Player {
        return if (Tts.audioUrl.isEmpty()) TtsPlayer(Looper.getMainLooper())
        else ExoPlayer.Builder(this@PlaybackService).build()
    }

    companion object {
        var isRunning = false

        var speechRate = 1f

        const val CUSTOM_COMMAND_REWIND_SEC = "CUSTOM_COMMAND_REWIND_SEC"
        const val CUSTOM_COMMAND_FORWARD_SEC = "CUSTOM_COMMAND_FORWARD_SEC"
    }



    @OptIn(UnstableApi::class)
    inner class TtsPlayer(looper: Looper) : SimpleBasePlayer(looper), OnInitListener {

        private var state = State.Builder()
            .setAvailableCommands(Commands.Builder().addAllCommands()
                .remove(COMMAND_SEEK_TO_NEXT)
                .remove(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .build())
            //.setPlayWhenReady(true, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(STATE_IDLE)
            //.setAudioAttributes(AudioAttributes.DEFAULT)
            //.setPlaylist(listOf(MediaItemData.Builder("test").build()))
            //.setPlaylistMetadata(
            //    MediaMetadata.Builder().setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
            ////        .setTitle("TTS test")
            //        .build()
            //)
            .build()

        private val utteranceListener = object :
            UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                L.d(">>>> TTS onStart")
            }

            override fun onDone(utteranceId: String) {
                L.d(">>>> TTS onDone")
                speakNextUtterance()
            }

            override fun onError(utteranceId: String) {
                L.d(">>>> TTS onError")
                updatePlaybackState(STATE_ENDED)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                L.d(">>>> TTS onError: $errorCode")
                updatePlaybackState(STATE_ENDED)
            }
        }

        override fun getState(): State {
            return state
        }

        override fun handleAddMediaItems(index: Int, mediaItems: MutableList<MediaItem>): ListenableFuture<*> {
            L.d(">>>> handleAddMediaItems")
            return Futures.immediateVoidFuture()
        }

        override fun handleSetMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> {
            L.d(">>>> handleSetMediaItems")
            Handler(Looper.getMainLooper()).post {

                val totalDuration = max(Tts.utterances.size * 10_000L, 10_000L)

                state = state.buildUpon()
                    .setPlaylist(listOf(MediaItemData
                        .Builder("test")
                        .setMediaItem(mediaItems[0])
                        .setIsSeekable(true)
                        .setDurationUs(totalDuration * 1000)
                        .build()))
                    .setIsLoading(false)
                    .setContentPositionMs(0)
                    .build()
                invalidateState()
            }
            return Futures.immediateVoidFuture()
        }

        override fun handleSetPlaylistMetadata(playlistMetadata: MediaMetadata): ListenableFuture<*> {
            L.d(">>>> handleSetPlaylistMetadata")
            return Futures.immediateVoidFuture()
        }

        override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
            L.d(">>>> handleSetPlaybackParameters")

            textToSpeech?.stop()
            textToSpeech?.setSpeechRate(playbackParameters.speed)
            if (playWhenReady) {
                speakCurrentUtterance()
            }

            return Futures.immediateVoidFuture()
        }

        override fun handlePrepare(): ListenableFuture<*> {
            L.d(">>>> handlePrepare")

            if (playWhenReady) {
                if (textToSpeech != null) {
                    speakCurrentUtterance()
                }
                updatePlaybackState(STATE_READY, true)
            } else {
                textToSpeech?.stop()
                updatePlaybackState(STATE_READY, false)
            }

            if (textToSpeech != null) {
                return Futures.immediateVoidFuture()
            }

            return CallbackToFutureAdapter.getFuture { completer ->
                textToSpeech = TextToSpeech(WikipediaApp.instance) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        textToSpeech?.setOnUtteranceProgressListener(utteranceListener)
                        textToSpeech?.setLanguage(Locale.getDefault())
                        textToSpeech?.setSpeechRate(speechRate)

                        if (playWhenReady) {
                            speakCurrentUtterance()
                            updatePlaybackState(STATE_READY, true)
                        }

                    } else {
                        L.d(">>>> Failed to initialize TTS")
                        textToSpeech = null
                    }
                    completer.set(Unit)
                }
                Unit
            }
        }

        override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
            L.d(">>>> handleSetPlayWhenReady: $playWhenReady")

            if (playWhenReady) {
                if (textToSpeech?.isSpeaking == false) {
                    speakCurrentUtterance()
                }
                updatePlaybackState(STATE_READY, true)
            } else {
                if (textToSpeech?.isSpeaking == true) {
                    textToSpeech?.stop()
                }
                updatePlaybackState(STATE_READY, false)
            }

            return Futures.immediateVoidFuture()
        }

        override fun handleRelease(): ListenableFuture<*> {
            L.d(">>>> handleRelease")
            textToSpeech?.stop()
            return Futures.immediateVoidFuture()
        }

        override fun handleStop(): ListenableFuture<*> {
            L.d(">>>> handleStop")
            textToSpeech?.stop()
            return Futures.immediateVoidFuture()
        }

        override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
            L.d(">>>> handleSeek: $positionMs")

            if (seekCommand == COMMAND_SEEK_TO_DEFAULT_POSITION) {
                Tts.currentUtterance = 0
                updatePlaybackPosition(0)
                if (playWhenReady) {
                    speakCurrentUtterance()
                }
            } else if (seekCommand == COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) {
                Tts.currentUtterance = (positionMs / 10_000).toInt()
                updatePlaybackPosition(Tts.currentUtterance * 10_000L)
                if (playWhenReady) {
                    speakCurrentUtterance()
                }
            }

            return Futures.immediateVoidFuture()
        }

        override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
            L.d(">>>> handleSetShuffleModeEnabled: $shuffleModeEnabled")
            return Futures.immediateVoidFuture()
        }

        override fun onInit(status: Int) {
            L.d(">>>> onInit: $status")
        }

        private fun updatePlaybackState(playbackState: Int, playWhenReady: Boolean = true) {
            L.d(">>>> updatePlaybackState: $playbackState")

            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                state = state.buildUpon()
                    .setPlaybackState(playbackState)
                    .setPlayWhenReady(playWhenReady, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                    .build()
                invalidateState()
            }
        }

        private fun updatePlaybackPosition(positionMs: Long) {
            L.d(">>>> updatePlaybackPosition: $positionMs")

            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                state = state.buildUpon()
                    .setContentPositionMs(positionMs)
                    .build()
                invalidateState()
            }
        }

        fun speakPrevUtterance() {
            Tts.currentUtterance--
            if (Tts.currentUtterance < 0) {
                Tts.currentUtterance = 0
            }
            speakCurrentUtterance()
        }

        fun speakNextUtterance() {
            Tts.currentUtterance++
            speakCurrentUtterance()
        }

        private fun speakCurrentUtterance() {
            val text = Tts.utterances.getOrNull(Tts.currentUtterance).orEmpty()
            if (text.isEmpty()) {
                updatePlaybackState(STATE_ENDED)
                Tts.currentUtterance = 0
                return
            }

            updatePlaybackPosition(Tts.currentUtterance * 10_000L)

            textToSpeech?.stop()
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID)
        }

    }
}

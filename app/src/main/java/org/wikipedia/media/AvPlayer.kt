package org.wikipedia.media

class AvPlayer {
    interface Callback {
        fun onSuccess()
        fun onError()
    }

    val isPlaying: Boolean
        get() = player.isPlaying

    private val player: MediaPlayerImplementation = MediaPlayerImplementation()

    fun deinit() {
        player.deinit()
    }

    fun play(path: String, callback: Callback) {
        player.load(path, object : Callback {
            override fun onSuccess() {
                player.play(callback)
            }

            override fun onError() {
                callback.onError()
            }
        })
    }

    fun pause() {
        player.pause()
    }

    fun stop() {
        player.stop()
    }
}

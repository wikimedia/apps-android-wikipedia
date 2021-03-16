package org.wikipedia.media

class AvPlayer {
    interface Callback {
        fun onSuccess()
        fun onError()
    }

    private val player = MediaPlayerImplementation()
    val isPlaying get() = player.isPlaying

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

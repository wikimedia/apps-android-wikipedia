package org.wikipedia.media

class AvPlayer {
    interface Callback {
        fun onSuccess()
        fun onError(code: Int, extra: Int)
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

            override fun onError(code: Int, extra: Int) {
                callback.onError(code, extra)
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

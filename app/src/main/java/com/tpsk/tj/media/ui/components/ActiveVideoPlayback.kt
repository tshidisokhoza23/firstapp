package com.tpsk.tj.media.ui.components

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.tpsk.tj.media.data.ShortsVideoCache

/**
 * Only one chat/media video may own audio. A previous player that was left
 * attached to a PlayerView keeps playing on some devices until the process dies.
 */
@UnstableApi
object ActiveVideoPlayback {
    private val players = mutableSetOf<ExoPlayer>()

    fun claim(player: ExoPlayer) {
        synchronized(players) {
            players.toList().forEach { other ->
                if (other !== player) {
                    runCatching {
                        other.playWhenReady = false
                        other.pause()
                    }
                }
            }
            players.add(player)
        }
        runCatching { ShortsVideoCache.pauseAll() }
    }

    fun detachAndRelease(view: PlayerView?, player: ExoPlayer) {
        synchronized(players) { players.remove(player) }
        runCatching { view?.player = null }
        runCatching {
            player.playWhenReady = false
            player.pause()
            player.clearVideoSurface()
            player.stop()
            player.clearMediaItems()
            player.release()
        }
    }
}

package com.tpsk.tj.chat.data

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import org.webrtc.EglBase
import org.webrtc.EglRenderer
import org.webrtc.GlRectDrawer
import org.webrtc.VideoFrame
import org.webrtc.VideoSink

/**
 * Normal view renderer. SurfaceViewRenderer draws in a separate window layer, so the
 * small call window stayed invisible until a tap and could not be dragged.
 */
class CallVideoRenderer(context: Context) : TextureView(context), VideoSink, TextureView.SurfaceTextureListener {
    private val drawer = GlRectDrawer()
    private val eglRenderer = EglRenderer("CallVideoRenderer")
    private var initialized = false
    private var mirror = false

    fun init(sharedContext: EglBase.Context) {
        if (initialized) return
        eglRenderer.init(sharedContext, EglBase.CONFIG_PLAIN, drawer)
        eglRenderer.setMirror(mirror)
        initialized = true
        surfaceTextureListener = this
        val texture = surfaceTexture
        if (isAvailable && texture != null) {
            onSurfaceTextureAvailable(texture, width, height)
        }
    }

    fun setMirror(enabled: Boolean) {
        mirror = enabled
        if (initialized) eglRenderer.setMirror(enabled)
    }

    override fun onFrame(frame: VideoFrame) {
        if (initialized) eglRenderer.onFrame(frame)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (!initialized) return
        eglRenderer.createEglSurface(Surface(surface))
        updateAspect(width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        updateAspect(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        if (initialized) {
            eglRenderer.releaseEglSurface { }
        }
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    fun releaseRenderer() {
        if (!initialized) return
        initialized = false
        eglRenderer.release()
        drawer.release()
    }

    private fun updateAspect(width: Int, height: Int) {
        if (height <= 0) return
        eglRenderer.setLayoutAspectRatio(width.toFloat() / height.toFloat())
    }
}

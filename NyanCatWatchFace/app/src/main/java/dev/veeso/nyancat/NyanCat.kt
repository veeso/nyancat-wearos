package dev.veeso.nyancat

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.Log

class NyanCat(resources: Resources, frames: List<Int>) {

    private val bitmaps: List<Bitmap>
    private var frames: List<Bitmap>
    private var currentFrame: Int
    private var currentFrameTick: Int

    init {
        this.bitmaps = frames.map {
            BitmapFactory.decodeResource(resources, it)
        }
        this.frames = bitmaps
        this.currentFrame = 0
        this.currentFrameTick = -1
        Log.d(TAG, "Initialized NyanCat with $this.frames.size frames")
    }

    fun draw(canvas: Canvas) {
        tick()
        canvas.drawBitmap(frames[currentFrame], 0f, 0f, null)
    }

    fun onSurfaceChanged(width: Int) {
        this.frames = this.frames.map {
            val scale = width.toFloat() / it.width.toFloat()
            Bitmap.createScaledBitmap(
                it,
                (it.width * scale).toInt(),
                (it.height * scale).toInt(), true
            )
        }
    }

    private fun tick() {
        currentFrame = ++currentFrame % frames.size
    }

    companion object {
        const val TAG: String = "NyanCat"
    }

}

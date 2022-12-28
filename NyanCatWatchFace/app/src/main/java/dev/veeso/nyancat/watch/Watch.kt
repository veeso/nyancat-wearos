package dev.veeso.nyancat.watch

import android.graphics.Canvas
import java.util.Calendar

interface Watch {

    fun render(canvas: Canvas, calendar: Calendar, ambientMode: Boolean)

    fun onSurfaceChanged(width: Int, height: Int)

    fun onMutedChanged(muted: Boolean)

    fun onAmbientModeChanged(ambient: Boolean)

}


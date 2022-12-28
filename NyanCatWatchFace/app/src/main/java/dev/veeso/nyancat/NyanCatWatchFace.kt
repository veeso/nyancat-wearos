@file:Suppress("DEPRECATION")

package dev.veeso.nyancat

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.SurfaceHolder
import dev.veeso.nyancat.watch.AnalogWatch
import dev.veeso.nyancat.watch.Watch
import dev.veeso.nyancat.watch.analog.AnalogWatchConfiguration
import java.lang.ref.WeakReference
import java.util.*


/**
 * Handler message id for updating the time periodically in interactive mode.
 */
private const val MSG_UPDATE_TIME = 0

@Suppress("DEPRECATION")
class NyanCatWatchFace : CanvasWatchFaceService() {

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: NyanCatWatchFace.Engine) : Handler(Looper.myLooper()!!) {
        private val mWeakReference: WeakReference<NyanCatWatchFace.Engine> =
            WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = mWeakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        private lateinit var calendar: Calendar
        private lateinit var watch: Watch
        private lateinit var nyanCat: NyanCat

        private var registeredTimeZoneReceiver = false
        private var muteMode: Boolean = false
        private var centerX: Float = 0F
        private var centerY: Float = 0F


        private lateinit var backgroundPaint: Paint
        private lateinit var backgroundBitmap: Bitmap
        private lateinit var grayBackgroundBitmap: Bitmap

        private var ambient: Boolean = false
        private var lowBitAmbient: Boolean = false
        private var burnInProtection: Boolean = false

        /* Handler to update the time once a second in interactive mode. */
        private val updateTimeHandler = EngineHandler(this)

        private val timeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }
        
        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(
                WatchFaceStyle.Builder(this@NyanCatWatchFace)
                    .setAcceptsTapEvents(true)
                    .build()
            )

            calendar = Calendar.getInstance()

            initializeBackground()
            initializeNyanCat()
            initializeWatch()
        }

        private fun initializeBackground() {
            backgroundPaint = Paint().apply {
                color = Color.BLACK
            }
            backgroundBitmap =
                BitmapFactory.decodeResource(resources, R.drawable.nyancat_00)
            Log.d(TAG, "Background initialized")

        }

        private fun initializeWatch() {
            watch = AnalogWatch(AnalogWatchConfiguration(), backgroundBitmap)
            Log.d(TAG, "Clock initialized")
        }

        private fun initializeNyanCat() {
            nyanCat = NyanCat(
                resources, listOf(
                    R.drawable.nyancat_00,
                    R.drawable.nyancat_01,
                    R.drawable.nyancat_02,
                    R.drawable.nyancat_03,
                    R.drawable.nyancat_04,
                    R.drawable.nyancat_05,
                    R.drawable.nyancat_06,
                    R.drawable.nyancat_07,
                    R.drawable.nyancat_08,
                    R.drawable.nyancat_09,
                    R.drawable.nyancat_10,
                    R.drawable.nyancat_11,
                    R.drawable.nyancat_12,
                    R.drawable.nyancat_13,
                    R.drawable.nyancat_14,
                    R.drawable.nyancat_15,
                )
            )
            Log.d(TAG, "Nyan cat initialized")
        }


        override fun onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            lowBitAmbient = properties.getBoolean(
                WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false
            )
            burnInProtection = properties.getBoolean(
                WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false
            )
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            ambient = inAmbientMode
            watch.onAmbientModeChanged(ambient)

            Log.d(TAG, "Ambient mode changed $inAmbientMode")

            // Check and trigger whether or not timer should be running (only
            // in active mode).
            updateTimer()
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            super.onInterruptionFilterChanged(interruptionFilter)
            val inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE

            Log.d(TAG, "Interruption filter changed $interruptionFilter")

            /* Dim display in mute mode. */
            if (muteMode != inMuteMode) {
                muteMode = inMuteMode
                watch.onMutedChanged(muteMode)
                invalidate()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            Log.d(TAG, "Surface changed $width x $height")

            centerX = width / 2f
            centerY = height / 2f

            watch.onSurfaceChanged(width, height)
            nyanCat.onSurfaceChanged(width)

            val scale = width.toFloat() / backgroundBitmap.width.toFloat()

            backgroundBitmap = Bitmap.createScaledBitmap(
                backgroundBitmap,
                (backgroundBitmap.width * scale).toInt(),
                (backgroundBitmap.height * scale).toInt(), true
            )

            if (!burnInProtection && !lowBitAmbient) {
                initGrayBackgroundBitmap()
            }
        }

        private fun initGrayBackgroundBitmap() {
            grayBackgroundBitmap = Bitmap.createBitmap(
                backgroundBitmap.width,
                backgroundBitmap.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(grayBackgroundBitmap)
            val grayPaint = Paint()
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            val filter = ColorMatrixColorFilter(colorMatrix)
            grayPaint.colorFilter = filter
            canvas.drawBitmap(backgroundBitmap, 0f, 0f, grayPaint)
        }

        /**
         * Captures tap event (and tap type). The [WatchFaceService.TAP_TYPE_TAP] case can be
         * used for implementing specific logic to handle the gesture.
         */
        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            Log.d(TAG, "Tap command on ${x}x$y")
            when (tapType) {
                WatchFaceService.TAP_TYPE_TOUCH -> {
                    // The user has started touching the screen.
                }
                WatchFaceService.TAP_TYPE_TOUCH_CANCEL -> {
                    // The user has started a different gesture or otherwise cancelled the tap.
                }
                WatchFaceService.TAP_TYPE_TAP -> {
                    // The user has completed the tap gesture.
                }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            calendar.timeInMillis = now
            // draw background
            drawBackground(canvas)
            // nyan cat
            nyanCat.draw(canvas)
            // render watch
            watch.render(canvas, calendar, ambient)
        }

        private fun drawBackground(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(backgroundBitmap, 0f, 0f, null)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                /* Update time zone in case it changed while we weren"t visible. */
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer()
        }

        private fun registerReceiver() {
            if (registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@NyanCatWatchFace.registerReceiver(timeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = false
            this@NyanCatWatchFace.unregisterReceiver(timeZoneReceiver)
        }

        /**
         * Starts/stops the [.mUpdateTimeHandler] timer based on the state of the watch face.
         */
        private fun updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.mUpdateTimeHandler] timer should be running. The timer
         * should only run in active mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !ambient
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val interval = clockInterval()
                val timeMs = System.currentTimeMillis()
                val delayMs =
                    interval - timeMs % interval
                updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }

        private fun clockInterval(): Int {
            return if (ambient) DEFAULT_UPDATE_RATE_MS else ANALOG_INTERACTIVE_UPDATE_RATE_MS
        }
    }

    companion object {
        const val ANALOG_INTERACTIVE_UPDATE_RATE_MS = 25
        const val DEFAULT_UPDATE_RATE_MS = 1000
        const val TAG = "NyanCatWatchFace"
    }
}

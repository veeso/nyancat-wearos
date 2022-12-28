package dev.veeso.nyancat.watch

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import androidx.palette.graphics.Palette
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import dev.veeso.nyancat.watch.analog.AnalogWatchConfiguration

private const val HOUR_STROKE_WIDTH = 8f
private const val MINUTE_STROKE_WIDTH = 8f
private const val SECOND_TICK_STROKE_WIDTH = 2f
private const val SECOND_BACK_TICK_STROKE_WIDTH = 4f
private const val SHADOW_RADIUS = 6f
private const val CENTER_GAP_AND_CIRCLE_RADIUS = 5f
private const val TEXT_SIZE = 24f
private const val TEXT_Y_PADDING = TEXT_SIZE / 4f

class AnalogWatch(configuration: AnalogWatchConfiguration, background: Bitmap) : Watch {

    private val configuration: AnalogWatchConfiguration

    private var centerX: Float = 0F
    private var centerY: Float = 0F

    private var secondHandLength: Float = 0F
    private var secondBackHandLength: Float = 0F
    private var minuteHandLength: Float = 0F
    private var hourHandLength: Float = 0F

    private var watchHandColor: Int = 0
    private var minorTicksColor: Int = 0
    private var watchHandHighlightColor: Int = 0
    private var watchHandShadowColor: Int = 0

    private lateinit var hourPaint: Paint
    private lateinit var minutePaint: Paint
    private lateinit var secondPaint: Paint
    private lateinit var secondBackPaint: Paint
    private lateinit var tickAndCirclePaint: Paint
    private lateinit var minorTickPaint: Paint
    private lateinit var hourTextPaint: Paint

    init {
        this.configuration = configuration

        Palette.from(background).generate {
            it?.let {
                watchHandHighlightColor = it.getVibrantColor(Color.RED)
                watchHandColor = it.getLightVibrantColor(Color.WHITE)
                watchHandShadowColor = it.getDarkMutedColor(Color.BLACK)
                minorTicksColor = it.getLightVibrantColor(Color.GRAY)
                onAmbientModeChanged(false)
            }
        }

        initializeWatchFace()
    }

    override fun render(canvas: Canvas, calendar: Calendar, ambientMode: Boolean) {
        val innerTickRadius = centerX - 10
        val outerTickRadius = centerX
        val textTickRadius = centerX - TEXT_SIZE - 8
        // hours ticks
        for (tickIndex in 0..11) {
            val tickRot = (tickIndex.toDouble() * Math.PI * 2.0 / 12).toFloat()
            val innerX = sin(tickRot.toDouble()).toFloat() * innerTickRadius
            val innerY = (-cos(tickRot.toDouble())).toFloat() * innerTickRadius
            val outerX = sin(tickRot.toDouble()).toFloat() * outerTickRadius
            val outerY = (-cos(tickRot.toDouble())).toFloat() * outerTickRadius

            canvas.drawLine(
                centerX + innerX, centerY + innerY,
                centerX + outerX, centerY + outerY, tickAndCirclePaint
            )
            if (configuration.showHoursText) {
                // draw hour text
                val hour = if (tickIndex == 0) {
                    12
                } else {
                    tickIndex
                }.toString()

                val textX = (sin(tickRot.toDouble()).toFloat() * textTickRadius)
                val textY = ((-cos(tickRot.toDouble())).toFloat() * textTickRadius) + TEXT_Y_PADDING

                canvas.drawText(
                    hour,
                    centerX + textX,
                    centerY + textY,
                    hourTextPaint
                )
            }
        }
        // minute ticks
        if (configuration.showMinuteTicks) {
            for (tickIndex in 0..59) {
                if (tickIndex % 5 == 0) {
                    continue
                }
                val tickRot = (tickIndex.toDouble() * Math.PI * 2.0 / 60).toFloat()
                val innerX = sin(tickRot.toDouble()).toFloat() * innerTickRadius
                val innerY = (-cos(tickRot.toDouble())).toFloat() * innerTickRadius
                val outerX = sin(tickRot.toDouble()).toFloat() * outerTickRadius
                val outerY = (-cos(tickRot.toDouble())).toFloat() * outerTickRadius

                canvas.drawLine(
                    centerX + innerX, centerY + innerY,
                    centerX + outerX, centerY + outerY, minorTickPaint
                )
            }
        }

        /*
         * These calculations reflect the rotation in degrees per unit of time, e.g.,
         * 360 / 60 = 6 and 360 / 12 = 30.
         */
        val seconds =
            calendar.get(Calendar.SECOND) + calendar.get(Calendar.MILLISECOND) / 1000f
        val secondsRotation = seconds * 6f

        val minutes = calendar.get(Calendar.MINUTE) + calendar.get(Calendar.SECOND) / 60f
        val minutesRotation = minutes * 6f

        val hourHandOffset = calendar.get(Calendar.MINUTE) / 2f
        val hoursRotation = calendar.get(Calendar.HOUR) * 30 + hourHandOffset

        /*
         * Save the canvas state before we can begin to rotate it.
         */
        canvas.save()

        canvas.rotate(hoursRotation, centerX, centerY)
        canvas.drawLine(
            centerX,
            centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
            centerX,
            centerY - hourHandLength,
            hourPaint
        )

        canvas.rotate(minutesRotation - hoursRotation, centerX, centerY)
        canvas.drawLine(
            centerX,
            centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
            centerX,
            centerY - minuteHandLength,
            minutePaint
        )

        /*
         * Ensure the "seconds" hand is drawn only when we are in interactive mode.
         * Otherwise, we only update the watch face once a minute.
         */
        if (!ambientMode && configuration.showSecondsHand) {
            canvas.rotate(secondsRotation - minutesRotation, centerX, centerY)
            canvas.drawLine(
                centerX,
                centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                centerX,
                centerY - secondHandLength,
                secondPaint
            )

            canvas.drawLine(
                centerX,
                centerY + CENTER_GAP_AND_CIRCLE_RADIUS,
                centerX,
                centerY + secondBackHandLength,
                secondBackPaint
            )

            canvas.drawCircle(
                centerX,
                centerY,
                CENTER_GAP_AND_CIRCLE_RADIUS * 2,
                secondPaint
            )

        }
        canvas.drawCircle(
            centerX,
            centerY,
            CENTER_GAP_AND_CIRCLE_RADIUS,
            tickAndCirclePaint
        )

        /* Restore the canvas" original orientation. */
        canvas.restore()
    }

    private fun initializeWatchFace() {
        /* Set defaults for colors */
        watchHandColor = Color.WHITE
        watchHandHighlightColor = Color.RED
        watchHandShadowColor = Color.BLACK

        hourPaint = Paint().apply {
            color = watchHandColor
            strokeWidth = HOUR_STROKE_WIDTH
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }

        minutePaint = Paint().apply {
            color = watchHandColor
            strokeWidth = MINUTE_STROKE_WIDTH
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }

        secondPaint = Paint().apply {
            color = watchHandHighlightColor
            strokeWidth = SECOND_TICK_STROKE_WIDTH
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }

        secondBackPaint = Paint().apply {
            color = watchHandHighlightColor
            strokeWidth = SECOND_BACK_TICK_STROKE_WIDTH
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }

        tickAndCirclePaint = Paint().apply {
            color = watchHandColor
            strokeWidth = SECOND_TICK_STROKE_WIDTH
            isAntiAlias = true
            style = Paint.Style.STROKE
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }

        minorTickPaint = Paint().apply {
            color = minorTicksColor
            strokeWidth = SECOND_TICK_STROKE_WIDTH
            isAntiAlias = true
            style = Paint.Style.STROKE
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }

        hourTextPaint = Paint().apply {
            color = watchHandColor
            strokeWidth = SECOND_TICK_STROKE_WIDTH
            isAntiAlias = true
            textSize = TEXT_SIZE
            textAlign = Align.CENTER
            style = Paint.Style.STROKE
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }
    }

    override fun onAmbientModeChanged(ambient: Boolean) {
        if (ambient) {
            hourPaint.color = Color.WHITE
            minutePaint.color = Color.WHITE
            secondPaint.color = Color.WHITE
            tickAndCirclePaint.color = Color.WHITE

            hourPaint.isAntiAlias = false
            minutePaint.isAntiAlias = false
            secondPaint.isAntiAlias = false
            tickAndCirclePaint.isAntiAlias = false
            minorTickPaint.isAntiAlias = false
            hourTextPaint.isAntiAlias = false

            hourPaint.clearShadowLayer()
            minutePaint.clearShadowLayer()
            secondPaint.clearShadowLayer()
            tickAndCirclePaint.clearShadowLayer()
            minorTickPaint.clearShadowLayer()
            hourTextPaint.clearShadowLayer()

        } else {
            hourPaint.color = watchHandColor
            minutePaint.color = watchHandColor
            secondPaint.color = watchHandHighlightColor
            tickAndCirclePaint.color = watchHandColor
            minorTickPaint.color = minorTicksColor
            hourTextPaint.color = watchHandColor

            hourPaint.isAntiAlias = true
            minutePaint.isAntiAlias = true
            secondPaint.isAntiAlias = true
            tickAndCirclePaint.isAntiAlias = true
            minorTickPaint.isAntiAlias = true
            hourTextPaint.isAntiAlias = true

            hourPaint.setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
            minutePaint.setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
            secondPaint.setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
            tickAndCirclePaint.setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
            minorTickPaint.setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
            hourTextPaint.setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }
    }

    override fun onMutedChanged(muted: Boolean) {
        hourPaint.alpha = if (muted) 100 else 255
        minutePaint.alpha = if (muted) 100 else 255
        secondPaint.alpha = if (muted) 80 else 255
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        centerX = width / 2f
        centerY = height / 2f

        secondHandLength = (centerX * 0.875).toFloat()
        secondBackHandLength = (centerX * 0.125).toFloat()
        minuteHandLength = (centerX * 0.875).toFloat()
        hourHandLength = (centerX * 0.425).toFloat()
    }

}

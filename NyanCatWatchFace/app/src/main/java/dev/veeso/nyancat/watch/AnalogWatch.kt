package dev.veeso.nyancat.watch

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Typeface
import androidx.palette.graphics.Palette
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import dev.veeso.nyancat.watch.analog.AnalogWatchConfiguration

class AnalogWatch(
    configuration: AnalogWatchConfiguration,
    background: Bitmap,
    watchFont: Typeface
) : Watch {

    private val configuration: AnalogWatchConfiguration
    private val watchFont: Typeface

    private var centerX: Float = 0F
    private var centerY: Float = 0F

    private var secondHandLength: Float = 0F
    private var secondBackHandLength: Float = 0F
    private var minuteHandLength: Float = 0F
    private var hourHandLength: Float = 0F

    private var watchHandColor: Int = 0
    private var watchHandBorderColor: Int = 0
    private var ticksColor: Int = 0
    private var minorTicksColor: List<Int>
    private var watchHandHighlightColor: Int = 0
    private var watchHandShadowColor: Int = 0

    private lateinit var hourPaint: Paint
    private lateinit var hourBorderPaint: Paint
    private lateinit var minutePaint: Paint
    private lateinit var minuteBorderPaint: Paint
    private lateinit var secondPaint: Paint
    private lateinit var secondBackPaint: Paint
    private lateinit var secondBorderPaint: Paint
    private lateinit var secondBackBorderPaint: Paint
    private lateinit var tickAndCirclePaint: Paint
    private lateinit var minorTickPaint: Paint
    private lateinit var hourTextPaint: Paint
    private lateinit var hourBorderTextPaint: Paint

    init {
        this.configuration = configuration

        watchHandColor = Color.WHITE
        watchHandBorderColor = Color.BLACK
        minorTicksColor = listOf(
            Color.rgb(252, 2, 4),
            Color.rgb(252, 152, 4),
            Color.rgb(252, 254, 4),
            Color.rgb(52, 254, 4),
            // Color.rgb(4, 150, 252),
            Color.rgb(101, 50, 252),
        )
        ticksColor = Color.rgb(252, 2, 4)

        this.watchFont = watchFont

        Palette.from(background).generate {
            it?.let {
                watchHandHighlightColor = it.getVibrantColor(Color.RED)
                watchHandShadowColor = it.getDarkMutedColor(Color.BLACK)
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
                    hourBorderTextPaint
                )

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

                minorTickPaint.color = minorTicksColor[tickIndex % 5]

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

        // minute
        canvas.rotate(minutesRotation - hoursRotation, centerX, centerY)

        canvas.drawLine(
            centerX,
            centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
            centerX,
            centerY - minuteHandLength,
            minuteBorderPaint
        )
        canvas.drawLine(
            centerX,
            centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
            centerX,
            centerY - minuteHandLength,
            minutePaint
        )

        // hours
        canvas.rotate(hoursRotation, centerX, centerY)

        canvas.drawLine(
            centerX,
            centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
            centerX,
            centerY - hourHandLength,
            hourBorderPaint
        )
        canvas.drawLine(
            centerX,
            centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
            centerX,
            centerY - hourHandLength,
            hourPaint
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
                secondBorderPaint
            )
            canvas.drawLine(
                centerX,
                centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                centerX,
                centerY - secondHandLength,
                secondPaint
            )

            // back
            canvas.drawLine(
                centerX,
                centerY + CENTER_GAP_AND_CIRCLE_RADIUS,
                centerX,
                centerY + secondBackHandLength,
                secondBackBorderPaint
            )
            canvas.drawLine(
                centerX,
                centerY + CENTER_GAP_AND_CIRCLE_RADIUS,
                centerX,
                centerY + secondBackHandLength,
                secondBackPaint
            )

            // circle
            canvas.drawCircle(
                centerX,
                centerY,
                CENTER_GAP_AND_CIRCLE_RADIUS * 2,
                secondBorderPaint
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
            strokeCap = Paint.Cap.SQUARE
            style = Paint.Style.FILL
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }

        hourBorderPaint = Paint().apply {
            color = watchHandBorderColor
            strokeWidth = HOUR_BORDER_STROKE_WIDTH
            isAntiAlias = true
            strokeCap = Paint.Cap.SQUARE
            style = Paint.Style.STROKE
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }

        minutePaint = Paint().apply {
            color = watchHandColor
            strokeWidth = MINUTE_STROKE_WIDTH
            isAntiAlias = true
            strokeCap = Paint.Cap.SQUARE
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }

        minuteBorderPaint = Paint().apply {
            color = watchHandBorderColor
            strokeWidth = MINUTE_BORDER_STROKE_WIDTH
            isAntiAlias = true
            strokeCap = Paint.Cap.SQUARE
            style = Paint.Style.STROKE
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }

        secondPaint = Paint().apply {
            color = watchHandHighlightColor
            strokeWidth = SECOND_TICK_STROKE_WIDTH
            isAntiAlias = true
            strokeCap = Paint.Cap.SQUARE
            style = Paint.Style.FILL
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }

        secondBorderPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = SECOND_TICK_STROKE_WIDTH + 4f
            isAntiAlias = true
            strokeCap = Paint.Cap.SQUARE
            style = Paint.Style.STROKE
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }

        secondBackPaint = Paint().apply {
            color = watchHandHighlightColor
            strokeWidth = SECOND_BACK_TICK_STROKE_WIDTH
            isAntiAlias = true
            strokeCap = Paint.Cap.SQUARE
            style = Paint.Style.FILL
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }

        secondBackBorderPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = SECOND_BACK_TICK_STROKE_WIDTH + 4f
            isAntiAlias = true
            strokeCap = Paint.Cap.SQUARE
            style = Paint.Style.STROKE
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }

        tickAndCirclePaint = Paint().apply {
            color = ticksColor
            strokeWidth = SECOND_TICK_STROKE_WIDTH
            isAntiAlias = true
            style = Paint.Style.STROKE
            setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
        }

        minorTickPaint = Paint().apply {
            color = Color.BLACK
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
            style = Paint.Style.FILL
            typeface = watchFont
        }

        hourBorderTextPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = SECOND_TICK_STROKE_WIDTH + 2f
            isAntiAlias = true
            textSize = TEXT_SIZE + 2f
            textAlign = Align.CENTER
            style = Paint.Style.STROKE
            typeface = watchFont
        }
    }

    override fun onAmbientModeChanged(ambient: Boolean) {
        if (ambient) {
            hourPaint.color = Color.WHITE
            minutePaint.color = Color.WHITE
            secondPaint.color = Color.WHITE
            tickAndCirclePaint.color = ticksColor

            hourPaint.isAntiAlias = false
            hourBorderPaint.isAntiAlias = false
            minutePaint.isAntiAlias = false
            minuteBorderPaint.isAntiAlias = false
            secondPaint.isAntiAlias = false
            secondBorderPaint.isAntiAlias = false
            secondBackBorderPaint.isAntiAlias = false
            secondBackPaint.isAntiAlias = false
            tickAndCirclePaint.isAntiAlias = false
            minorTickPaint.isAntiAlias = false
            hourTextPaint.isAntiAlias = false
            hourBorderTextPaint.isAntiAlias = false

            hourPaint.clearShadowLayer()
            hourBorderPaint.clearShadowLayer()
            minutePaint.clearShadowLayer()
            minuteBorderPaint.clearShadowLayer()
            secondPaint.clearShadowLayer()
            secondBackBorderPaint.clearShadowLayer()
            secondBackPaint.clearShadowLayer()
            secondBorderPaint.clearShadowLayer()
            tickAndCirclePaint.clearShadowLayer()
            minorTickPaint.clearShadowLayer()

        } else {
            hourPaint.color = watchHandColor
            minutePaint.color = watchHandColor
            secondPaint.color = watchHandHighlightColor
            tickAndCirclePaint.color = ticksColor
            hourTextPaint.color = watchHandColor

            hourPaint.isAntiAlias = true
            hourBorderPaint.isAntiAlias = true
            minutePaint.isAntiAlias = true
            minuteBorderPaint.isAntiAlias = true
            secondPaint.isAntiAlias = true
            secondBorderPaint.isAntiAlias = true
            secondBackBorderPaint.isAntiAlias = true
            secondBackPaint.isAntiAlias = true
            tickAndCirclePaint.isAntiAlias = true
            minorTickPaint.isAntiAlias = true
            hourTextPaint.isAntiAlias = true
            hourBorderPaint.isAntiAlias = true

            hourPaint.setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
            hourBorderPaint.setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
            minutePaint.setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
            minuteBorderPaint.setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
            secondPaint.setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
            secondBorderPaint.setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
            secondBackPaint.setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
            secondBackBorderPaint.setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
            tickAndCirclePaint.setShadowLayer(
                SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
            )
            minorTickPaint.setShadowLayer(
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

    companion object {
        const val HOUR_STROKE_WIDTH = 8f
        const val HOUR_BORDER_STROKE_WIDTH = HOUR_STROKE_WIDTH + 8f
        const val MINUTE_STROKE_WIDTH = HOUR_STROKE_WIDTH
        const val MINUTE_BORDER_STROKE_WIDTH = MINUTE_STROKE_WIDTH + 6f
        const val SECOND_TICK_STROKE_WIDTH = 2f
        const val SECOND_BACK_TICK_STROKE_WIDTH = 4f
        const val SHADOW_RADIUS = 6f
        const val CENTER_GAP_AND_CIRCLE_RADIUS = 5f
        const val TEXT_SIZE = 24f
        const val TEXT_Y_PADDING = 16f
    }

}

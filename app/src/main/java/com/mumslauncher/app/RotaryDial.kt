package com.mumslauncher.app

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val DIGITS = charArrayOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0')

private val HOLE_ANGLES = floatArrayOf(120f, 90f, 60f, 30f, 0f, 330f, 300f, 270f, 240f, 210f)
private const val FINGER_STOP_ANGLE = 150f

private val MAX_ROTATION = FloatArray(HOLE_ANGLES.size) { i ->
    ((FINGER_STOP_ANGLE - HOLE_ANGLES[i] + 360f) % 360f)
}
private const val STOP_TOLERANCE = 12f
private const val TICK_INTERVAL_DEG = 18f

private val DIAL_COLOR = Color(0xFF2C2C2C)
private val HOLE_COLOR = Color.White
private val HOLE_SHADOW_COLOR = Color(0x55000000)
private val STOP_COLOR = Color(0xFF888888)
private val HUB_INNER_COLOR = Color(0xFF3A3A3A)
private val NUMBER_COLOR = Color(0xFF1A1A1A)

private val HAPTIC_ATTRS by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        VibrationAttributes.Builder()
            .setUsage(VibrationAttributes.USAGE_PHYSICAL_EMULATION)
            .build()
    } else null
}

private fun buzz(vib: Vibrator, durationMs: Long, amplitude: Int) {
    if (amplitude <= 0) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = VibrationEffect.createOneShot(durationMs, amplitude)
        val attrs = HAPTIC_ATTRS
        if (attrs != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vib.vibrate(effect, attrs)
        } else {
            vib.vibrate(effect)
        }
    }
}

private fun buzzTick(vib: Vibrator, amplitude: Int)  = buzz(vib, 10L, amplitude)
private fun buzzClack(vib: Vibrator, amplitude: Int) = buzz(vib, 22L, amplitude)

private fun vibrator(context: Context): Vibrator =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

@Composable
fun RotaryDial(
    modifier: Modifier = Modifier,
    stiffness: Float = 80f,
    hapticStrength: Float = 200f,
    onDigitDialled: (Char) -> Unit,
    centerContent: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val vib = remember { vibrator(context) }
    val currentStiffness = rememberUpdatedState(stiffness)
    val currentHaptic    = rememberUpdatedState(hapticStrength)

    val textMeasurer = rememberTextMeasurer()
    val textStyle = remember {
        TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = NUMBER_COLOR
        )
    }
    val textLayouts = remember(textMeasurer, textStyle) {
        DIGITS.map { d -> textMeasurer.measure(d.toString(), textStyle) }
    }

    val rotationAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.aspectRatio(1f)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val outerR = min(size.width, size.height) / 2f * 0.9f
                    val orbitR = outerR * 0.68f
                    val tapR   = outerR * 0.16f

                    var activeIdx = -1
                    var localMaxRot = 0f
                    var lastClock = 0f
                    var reached = false
                    var currentRotation = 0f
                    var lastTickAt = 0f
                    var atStop = false

                    detectDragGestures(
                        onDragStart = { offset ->
                            activeIdx = -1
                            reached = false
                            currentRotation = 0f
                            lastTickAt = 0f
                            atStop = false
                            for (i in HOLE_ANGLES.indices) {
                                val rad = Math.toRadians(HOLE_ANGLES[i].toDouble())
                                val hx = cx + orbitR * sin(rad).toFloat()
                                val hy = cy - orbitR * cos(rad).toFloat()
                                if ((offset - Offset(hx, hy)).getDistance() < tapR) {
                                    activeIdx = i
                                    localMaxRot = MAX_ROTATION[i]
                                    break
                                }
                            }
                            lastClock = clockAngle(offset.x - cx, offset.y - cy)
                            scope.launch {
                                rotationAnim.stop()
                                rotationAnim.snapTo(0f)
                            }
                        },
                        onDrag = { change, _ ->
                            if (activeIdx == -1) return@detectDragGestures
                            change.consume()
                            val currentClock = clockAngle(change.position.x - cx, change.position.y - cy)
                            var delta = ((currentClock - lastClock + 360f) % 360f)
                            if (delta > 180f) delta -= 360f
                            lastClock = currentClock

                            val newVal = (currentRotation + delta).coerceIn(0f, localMaxRot)
                            currentRotation = newVal
                            scope.launch { rotationAnim.snapTo(newVal) }

                            val nowAtStop = newVal >= localMaxRot - STOP_TOLERANCE
                            if (nowAtStop && !atStop) {
                                buzzClack(vib, currentHaptic.value.toInt())
                            }
                            atStop = nowAtStop
                            reached = nowAtStop

                            if (delta > 0f && newVal - lastTickAt >= TICK_INTERVAL_DEG) {
                                lastTickAt = newVal
                                buzzTick(vib, currentHaptic.value.toInt() / 3)
                            }
                        },
                        onDragEnd = {
                            if (activeIdx != -1) {
                                val capturedIdx     = activeIdx
                                val capturedReached = reached
                                activeIdx = -1
                                if (capturedReached) onDigitDialled(DIGITS[capturedIdx])
                                scope.launch {
                                    rotationAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = currentStiffness.value
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            activeIdx = -1
                            scope.launch {
                                rotationAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = currentStiffness.value
                                    )
                                )
                            }
                        }
                    )
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val center = Offset(cx, cy)
            val outerR = min(size.width, size.height) / 2f * 0.9f
            val orbitR = outerR * 0.68f
            val holeR  = outerR * 0.13f
            val hubR   = outerR * 0.32f

            val textScale = holeR * 1.6f / (textLayouts[0].size.height.toFloat().coerceAtLeast(1f))

            drawCircle(color = DIAL_COLOR, radius = outerR, center = center)

            run {
                val rad = Math.toRadians(FINGER_STOP_ANGLE.toDouble())
                val sx = cx + (outerR - holeR * 0.9f) * sin(rad).toFloat()
                val sy = cy - (outerR - holeR * 0.9f) * cos(rad).toFloat()
                drawCircle(color = STOP_COLOR, radius = holeR * 0.55f, center = Offset(sx, sy))
            }

            withTransform({ rotate(rotationAnim.value, pivot = center) }) {
                for (i in HOLE_ANGLES.indices) {
                    val rad = Math.toRadians(HOLE_ANGLES[i].toDouble())
                    val hx = cx + orbitR * sin(rad).toFloat()
                    val hy = cy - orbitR * cos(rad).toFloat()
                    val hCenter = Offset(hx, hy)

                    drawCircle(color = HOLE_SHADOW_COLOR, radius = holeR + 3f, center = hCenter)
                    drawCircle(color = HOLE_COLOR, radius = holeR, center = hCenter)

                    val layout = textLayouts[i]
                    val tw = layout.size.width * textScale
                    val th = layout.size.height * textScale
                    withTransform({ scale(textScale, textScale, pivot = hCenter) }) {
                        drawText(layout, topLeft = Offset(hx - tw / textScale / 2f, hy - th / textScale / 2f))
                    }
                }
            }

            drawCircle(color = DIAL_COLOR, radius = hubR, center = center)
            drawCircle(color = HUB_INNER_COLOR, radius = hubR * 0.88f, center = center)
        }

        centerContent()
    }
}

private fun clockAngle(dx: Float, dy: Float): Float {
    val deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    return ((deg + 90f) % 360f + 360f) % 360f
}

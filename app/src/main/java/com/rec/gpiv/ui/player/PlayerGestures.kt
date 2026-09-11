package com.rec.gpiv.ui.player

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.rec.gpiv.player.MpvNative
import com.rec.gpiv.player.ZOOM_DOUBLE_TAP_DRAG
import com.rec.gpiv.player.BRIGHTNESS_PROP
import com.rec.gpiv.player.CONTRAST_PROP
import com.rec.gpiv.player.GAMMA_PROP
import com.rec.gpiv.player.SATURATION_PROP
import com.rec.gpiv.player.SEEK_GESTURE_SENSITIVITY
import com.rec.gpiv.player.SEEK_SECONDS

@Composable
fun PlayerGestures(
    mpvNative: MpvNative,
    gestureTool: GestureTool,
    onDoubleTap: () -> Unit,
    onTwoFingerTap: () -> Unit,
    onSeekBackward: (Double) -> Unit,
    onSeekForward: (Double) -> Unit,
    position: Double,
    duration: Double,
    onSeekTo: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 80.dp)
            .pointerInput(gestureTool) {

                awaitPointerEventScope {

                    var lastTapTime = 0L

                    while (true) {


                        val down = awaitFirstDown(
                            requireUnconsumed = false
                        )

                        val now =
                            System.currentTimeMillis()

                        val isDoubleTap =
                            now - lastTapTime < 300

                        lastTapTime = now

                        var previousPosition =
                            down.position

                        var moved = false

                        /*
                         * ==================================================
                         * SEGUNDO TAP MANTIDO
                         * ==================================================
                         */

                        if (isDoubleTap) {

                            var doubleTapMoved = false

                            var seekAccumulator = 0.0
                            var seekPercentAccumulator = 0.0
                            var seekPercent = 0.0

                            seekPercent =
                                if (duration > 0.0) {
                                    (position / duration * 100.0)
                                        .coerceIn(0.0, 100.0)
                                } else {
                                    0.0
                                }

                            while (true) {

                                val event =
                                    awaitPointerEvent(
                                        pass = PointerEventPass.Main
                                    )

                                val pressed =
                                    event.changes.filter {
                                        it.pressed
                                    }

                                if (pressed.size != 1) {
                                    break
                                }

                                val pointerPosition =
                                    pressed[0].position

                                val delta =
                                    pointerPosition -
                                        previousPosition

                                if (delta.y != 0f) {
                                    moved = true
                                    doubleTapMoved = true

                                    val propAmount =
                                        -delta.y / size.height.toDouble()

                                    when (gestureTool) {

                                        GestureTool.ZOOM -> {
                                            mpvNative.changeZoom(
                                                propAmount * ZOOM_DOUBLE_TAP_DRAG
                                            )
                                        }

                                        GestureTool.BRIGHTNESS -> {
                                            mpvNative.changeBrightness(
                                                propAmount * BRIGHTNESS_PROP
                                            )
                                        }

                                        GestureTool.CONTRAST -> {
                                            mpvNative.changeContrast(
                                                propAmount * CONTRAST_PROP
                                            )
                                        }

                                        GestureTool.GAMMA -> {
                                            mpvNative.changeGamma(
                                                propAmount * GAMMA_PROP
                                            )
                                        }

                                        GestureTool.SATURATION -> {
                                            mpvNative.changeSaturation(
                                                propAmount * SATURATION_PROP
                                            )
                                        }

                                        GestureTool.SEEK -> {
                                            seekAccumulator +=
                                                propAmount * SEEK_GESTURE_SENSITIVITY

                                            while (seekAccumulator >= 1.0) {
                                                onSeekForward(SEEK_SECONDS)
                                                seekAccumulator -= 1.0
                                            }

                                            while (seekAccumulator <= -1.0) {
                                                onSeekBackward(SEEK_SECONDS)
                                                seekAccumulator += 1.0
                                            }
                                        }

                                        GestureTool.SEEK_PERCENT -> {
                                            if (duration > 0.0) {

                                                seekPercentAccumulator +=
                                                    propAmount *
                                                        SEEK_GESTURE_SENSITIVITY

                                                while (seekPercentAccumulator >= 1.0) {

                                                    seekPercent =
                                                        if (seekPercent % 10.0 == 0.0) {
                                                            seekPercent + 10.0
                                                        } else {
                                                            kotlin.math.ceil(
                                                                seekPercent / 10.0
                                                            ) * 10.0
                                                        }

                                                    seekPercent =
                                                        seekPercent.coerceAtMost(100.0)

                                                    onSeekTo(
                                                        duration * seekPercent / 100.0
                                                    )

                                                    seekPercentAccumulator -= 1.0
                                                }

                                                while (seekPercentAccumulator <= -1.0) {

                                                    seekPercent =
                                                        if (seekPercent % 10.0 == 0.0) {
                                                            seekPercent - 10.0
                                                        } else {
                                                            kotlin.math.floor(
                                                                seekPercent / 10.0
                                                            ) * 10.0
                                                        }

                                                    seekPercent =
                                                        seekPercent.coerceAtLeast(0.0)

                                                    onSeekTo(
                                                        duration * seekPercent / 100.0
                                                    )

                                                    seekPercentAccumulator += 1.0
                                                }
                                            }
                                        }

                                        GestureTool.VOLUME -> {
                                            // Volume ainda não implementado.
                                        }

                                        GestureTool.NONE -> {
                                        }
                                    }
                                }

                                previousPosition =
                                    pointerPosition
                            }

                            if (!doubleTapMoved) {
                                onDoubleTap()
                            }

                            continue
                        }

                        /*
                         * ==================================================
                         * PRIMEIRO TAP / PAN
                         * ==================================================
                         */

                        while (true) {

                            val event =
                                awaitPointerEvent(
                                    pass = PointerEventPass.Main
                                )

                            val pressed =
                                event.changes.filter {
                                    it.pressed
                                }

                            /*
                             * Dois dedos:
                             * ignoramos completamente.
                             */

                            if (pressed.size >= 2) {
                                moved = true
                                onTwoFingerTap()
                                
                                while (true) {
                                    val twoFingerEvent =
                                        awaitPointerEvent(
                                            pass = PointerEventPass.Main
                                        )

                                    val pointers =
                                        twoFingerEvent.changes.filter {
                                            it.pressed
                                        }

                                    if (pointers.isEmpty()) {
                                        break
                                    }
                                }

                                break
                            }

                            /*
                             * Um dedo:
                             * pan direto.
                             */

                            if (pressed.size == 1) {

                                val position =
                                    pressed[0].position

                                val delta =
                                    position -
                                        previousPosition

                                if (
                                    gestureTool == GestureTool.ZOOM &&
                                    (delta.x != 0f || delta.y != 0f)
                                ) {

                                    moved = true

                                    val panX =
                                        delta.x /
                                            size.width.toFloat()

                                    val panY =
                                        delta.y /
                                            size.height.toFloat()

                                    mpvNative.pan(
                                        panX.toDouble(),
                                        panY.toDouble()
                                    )
                                }

                                previousPosition =
                                    position

                            } else {

                                /*
                                 * Dedo levantado.
                                 */

                                break
                            }
                        }
                    }
                }
            }
    )
}

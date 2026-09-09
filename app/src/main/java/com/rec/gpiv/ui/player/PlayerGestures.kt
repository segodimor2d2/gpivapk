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

@Composable
fun PlayerGestures(
    mpvNative: MpvNative,
    gestureTool: GestureTool,
    onDoubleTap: () -> Unit,
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

                                val position =
                                    pressed[0].position

                                val delta =
                                    position -
                                        previousPosition

                                /*
                                 * Arrastar para cima:
                                 * aumenta zoom.
                                 *
                                 * Arrastar para baixo:
                                 * diminui zoom.
                                 */

                                if (delta.y != 0f) {
                                    moved = true
                                    doubleTapMoved = true

                                    val zoomAmount =
                                        -delta.y / size.height.toFloat() *
                                        ZOOM_DOUBLE_TAP_DRAG

                                    when (gestureTool) {
                                        GestureTool.ZOOM -> {
                                            mpvNative.changeZoom(zoomAmount)
                                        }

                                        GestureTool.BRIGHTNESS -> {
                                            mpvNative.changeBrightness(zoomAmount)
                                        }

                                        GestureTool.CONTRAST -> {
                                            mpvNative.changeContrast(zoomAmount)
                                        }

                                        GestureTool.GAMMA -> {
                                            mpvNative.changeGamma(zoomAmount)
                                        }

                                        GestureTool.SATURATION -> {
                                            mpvNative.changeSaturation(zoomAmount)
                                        }

                                        GestureTool.VOLUME -> {
                                            // Volume ainda não implementado.
                                        }

                                        GestureTool.NONE -> {
                                        }
                                    }


                                }

                                previousPosition =
                                    position
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

                                while (true) {

                                    val twoFingerEvent =
                                        awaitPointerEvent(
                                            pass =
                                                PointerEventPass.Main
                                        )

                                    val pointers =
                                        twoFingerEvent.changes.filter {
                                            it.pressed
                                        }

                                    if (pointers.size != 1) {
                                        if (pointers.isEmpty()) {
                                            break
                                        }
                                        continue
                                    }

                                    previousPosition =
                                        pointers[0].position
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

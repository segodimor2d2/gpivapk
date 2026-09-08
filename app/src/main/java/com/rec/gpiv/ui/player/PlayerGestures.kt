package com.rec.gpiv.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.rec.gpiv.player.MpvNative
import kotlin.math.hypot
import kotlin.math.ln

@Composable
fun PlayerGestures(
    mpvNative: MpvNative,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 80.dp)
            .pointerInput(Unit) {

                awaitPointerEventScope {

                    while (true) {

                        /*
                         * Espera o primeiro dedo.
                         */
                        awaitFirstDown(
                            requireUnconsumed = false
                        )

                        var previousCentroid = Offset.Zero
                        var previousDistance = 0f
                        var trackingTwoPointers = false

                        /*
                         * Processa esta interação até
                         * todos os dedos serem retirados.
                         */
                        while (true) {

                            val event = awaitPointerEvent(
                                pass = PointerEventPass.Main
                            )

                            val pressedPointers =
                                event.changes.filter {
                                    it.pressed
                                }

                            /*
                             * ====================================================
                             * DOIS DEDOS
                             * ====================================================
                             */
                            if (pressedPointers.size >= 2) {

                                val first =
                                    pressedPointers[0].position

                                val second =
                                    pressedPointers[1].position

                                val centroid =
                                    Offset(
                                        x = (first.x + second.x) / 2f,
                                        y = (first.y + second.y) / 2f
                                    )

                                val distance =
                                    hypot(
                                        second.x - first.x,
                                        second.y - first.y
                                    )

                                /*
                                 * Primeiro evento com dois dedos:
                                 * somente estabelece a referência.
                                 */
                                if (!trackingTwoPointers) {

                                    previousCentroid = centroid
                                    previousDistance = distance
                                    trackingTwoPointers = true

                                } else {

                                    /*
                                     * ====================================================
                                     * PAN
                                     * ====================================================
                                     */
                                    val pan =
                                        centroid - previousCentroid

                                    if (
                                        pan.x != 0f ||
                                        pan.y != 0f
                                    ) {

                                        val panX =
                                            pan.x /
                                                size.width.toFloat()

                                        val panY =
                                            pan.y /
                                                size.height.toFloat()

                                        mpvNative.pan(
                                            panX.toDouble(),
                                            panY.toDouble()
                                        )
                                    }

                                    /*
                                     * ====================================================
                                     * ZOOM
                                     * ====================================================
                                     */
                                    if (previousDistance > 0f) {

                                        val zoomFactor =
                                            distance /
                                                previousDistance

                                        if (
                                            zoomFactor > 0f &&
                                            zoomFactor != 1f
                                        ) {

                                            val zoomAmount =
                                                ln(
                                                    zoomFactor.toDouble()
                                                ) /
                                                    ln(2.0)

                                            mpvNative.changeZoom(
                                                zoomAmount
                                            )
                                        }
                                    }

                                    previousCentroid = centroid
                                    previousDistance = distance
                                }

                            } else {

                                /*
                                 * Menos de dois dedos:
                                 * a próxima entrada do segundo dedo
                                 * começa uma nova referência.
                                 */
                                trackingTwoPointers = false
                            }

                            /*
                             * ====================================================
                             * TODOS OS DEDOS RETIRADOS
                             * ====================================================
                             */
                            if (pressedPointers.isEmpty()) {
                                break
                            }
                        }
                    }
                }
            }
    )
}

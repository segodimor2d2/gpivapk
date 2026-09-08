package com.rec.gpiv.ui.player

import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

import com.rec.gpiv.player.MpvNative

@Composable
fun PlayerSurface(
    mpvNative: MpvNative,
    onSurfaceReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    /*
     * ============================================================
     * RENDER LOOP
     * ============================================================
     */

    var renderLoopRunning by remember {
        mutableStateOf(false)
    }

    val renderHandler = remember(mpvNative) {
        Handler(
            Looper.getMainLooper()
        )
    }

    val renderRunnable = remember(mpvNative) {

        object : Runnable {

            override fun run() {

                if (!renderLoopRunning) {
                    return
                }

                mpvNative.render()

                renderHandler.postDelayed(
                    this,
                    16L
                )
            }
        }
    }

    /*
     * ============================================================
     * LIMPEZA
     * ============================================================
     */

    DisposableEffect(mpvNative) {

        onDispose {

            renderLoopRunning = false

            renderHandler.removeCallbacks(
                renderRunnable
            )
        }
    }

    /*
     * ============================================================
     * SURFACE
     * ============================================================
     */

    AndroidView(
        factory = { context ->

            SurfaceView(context).apply {

                holder.addCallback(
                    object : SurfaceHolder.Callback {

                        /*
                         * ------------------------------------------------
                         * SURFACE CREATED
                         * ------------------------------------------------
                         */

                        override fun surfaceCreated(
                            holder: SurfaceHolder
                        ) {

                            println(
                                "PlayerSurface: surfaceCreated()"
                            )

                            mpvNative.setSurface(
                                holder.surface
                            )

                            /*
                             * Inicia o render loop somente uma vez.
                             */

                            if (!renderLoopRunning) {

                                renderLoopRunning = true

                                renderHandler.post(
                                    renderRunnable
                                )
                            }

                            /*
                             * Informa ao PlayerScreen/ViewModel
                             * que a Surface está pronta.
                             */

                            onSurfaceReady()
                        }

                        /*
                         * ------------------------------------------------
                         * SURFACE CHANGED
                         * ------------------------------------------------
                         */

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) {

                            println(
                                "PlayerSurface: " +
                                    "surfaceChanged: " +
                                    "${width}x${height}"
                            )
                        }

                        /*
                         * ------------------------------------------------
                         * SURFACE DESTROYED
                         * ------------------------------------------------
                         */

                        override fun surfaceDestroyed(
                            holder: SurfaceHolder
                        ) {

                            println(
                                "PlayerSurface: surfaceDestroyed()"
                            )

                            renderLoopRunning = false

                            renderHandler.removeCallbacks(
                                renderRunnable
                            )

                            mpvNative.setSurface(
                                null
                            )
                        }
                    }
                )
            }
        },

        modifier = modifier
    )
}

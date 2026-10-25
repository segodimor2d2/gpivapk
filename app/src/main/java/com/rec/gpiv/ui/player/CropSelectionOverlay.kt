package com.rec.gpiv.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.rec.gpiv.model.CropRegion
import kotlin.math.abs

/** Drag inside to move; drag a corner to resize. Does not alter the playing image. */
@Composable
fun CropSelectionOverlay(region: CropRegion, onChange: (CropRegion) -> Unit) {
    val current by rememberUpdatedState(region)
    val update by rememberUpdatedState(onChange)
    Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
        var horizontal = 0
        var vertical = 0
        detectDragGestures(
            onDragStart = { point ->
                val r = current
                val radius = 32.dp.toPx()
                horizontal = when {
                    abs(point.x - r.left * size.width) < radius -> -1
                    abs(point.x - r.right * size.width) < radius -> 1
                    else -> 0
                }
                vertical = when {
                    abs(point.y - r.top * size.height) < radius -> -1
                    abs(point.y - r.bottom * size.height) < radius -> 1
                    else -> 0
                }
            },
            onDrag = { change, delta ->
                change.consume()
                val r = current
                val dx = delta.x / size.width
                val dy = delta.y / size.height
                val minWidth = (48.dp.toPx() / size.width).coerceAtMost(0.2f)
                val minHeight = (48.dp.toPx() / size.height).coerceAtMost(0.2f)
                if (horizontal == 0 && vertical == 0) {
                    val x = dx.coerceIn(-r.left, 1f - r.right)
                    val y = dy.coerceIn(-r.top, 1f - r.bottom)
                    update(CropRegion(r.left + x, r.top + y, r.right + x, r.bottom + y))
                } else {
                    update(CropRegion(
                        if (horizontal == -1) (r.left + dx).coerceIn(0f, r.right - minWidth) else r.left,
                        if (vertical == -1) (r.top + dy).coerceIn(0f, r.bottom - minHeight) else r.top,
                        if (horizontal == 1) (r.right + dx).coerceIn(r.left + minWidth, 1f) else r.right,
                        if (vertical == 1) (r.bottom + dy).coerceIn(r.top + minHeight, 1f) else r.bottom
                    ))
                }
            }
        )
    }) {
        val left = region.left * size.width
        val top = region.top * size.height
        val right = region.right * size.width
        val bottom = region.bottom * size.height
        val shade = Color.Black.copy(alpha = 0.45f)
        drawRect(shade, Offset.Zero, Size(size.width, top))
        drawRect(shade, Offset(0f, bottom), Size(size.width, size.height - bottom))
        drawRect(shade, Offset(0f, top), Size(left, bottom - top))
        drawRect(shade, Offset(right, top), Size(size.width - right, bottom - top))
        drawRect(Color.Yellow, Offset(left, top), Size(right - left, bottom - top), style = Stroke(2.dp.toPx()))
        listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom)).forEach {
            drawCircle(Color.Yellow, 6.dp.toPx(), it)
        }
    }
}

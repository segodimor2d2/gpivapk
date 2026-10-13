package com.rec.gpiv.ui.player

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rec.gpiv.ahp.AhpRanking
import com.rec.gpiv.model.PlayerUiState
import com.rec.gpiv.model.ThumbnailEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ThumbnailGallery(state: PlayerUiState, onDismiss: () -> Unit, onCompare: (Int) -> Unit, onFinish: () -> Unit) {
    val pair = if (state.ahpReady) AhpRanking.pair(state.thumbnails.size, state.ahpValues.size) else null
    val total = state.thumbnails.size.toLong() * (state.thumbnails.size - 1) / 2
    Dialog(onDismissRequest = { if (!state.ahpBusy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false,
        dismissOnBackPress = !state.ahpBusy, dismissOnClickOutside = !state.ahpBusy)) {
        Column(Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.94f).background(Color.Black).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("AHP • ${state.ahpValues.size} / $total", color = Color.White, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss, enabled = !state.ahpBusy) { Text("Fechar") }
            }
            state.ahpMessage?.let { Text(it, color = Color.White) }
            if (pair != null) {
                Text("Qual arquivo você prefere?", color = Color.LightGray)
                Preview(state.thumbnails[pair.first], "A", Modifier.weight(1f))
                Preview(state.thumbnails[pair.second], "B", Modifier.weight(1f))
                key(state.ahpValues.size) {
                    PreferenceSlider(!state.ahpBusy, onCompare)
                }
            } else {
                Spacer(Modifier.weight(1f))
                if (state.ahpReady && state.thumbnails.isNotEmpty()) {
                    TextButton(onClick = onFinish, enabled = !state.ahpBusy) {
                        Text(if (state.thumbnails.size == 1) "Renomear único arquivo" else "Tentar renomear novamente")
                    }
                }
            }
        }
    }
}

@Composable
private fun Preview(entry: ThumbnailEntry, label: String, modifier: Modifier) {
    val preview by produceState<ImageBitmap?>(null, entry.thumbnailPath) {
        value = withContext(Dispatchers.IO) { entry.thumbnailPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() } }
    }
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$label • ${entry.name}", color = Color.White, maxLines = 1)
        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF151515)), contentAlignment = Alignment.Center) {
            preview?.let { Image(it, "$label: ${entry.name}", Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                ?: Text("Prévia indisponível", color = Color.LightGray)
        }
    }
}

@Composable
private fun PreferenceSlider(enabled: Boolean, onCompare: (Int) -> Unit) {
    var selected by remember { mutableIntStateOf(0) }
    val submit by rememberUpdatedState(onCompare)
    Row(Modifier.fillMaxWidth().height(172.dp), horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.height(150.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("A 9", color = Color.White)
            Text("=", color = Color.White)
            Text("B 9", color = Color.White)
        }
        Canvas(Modifier.width(72.dp).height(160.dp).semantics {
            contentDescription = "Preferência A ou B. Centro: igualdade. Solte para salvar."
            progressBarRangeInfo = ProgressBarRangeInfo(selected.toFloat(), -8f..8f, 15)
            if (enabled) setProgress { value ->
                selected = value.roundToInt().coerceIn(-8, 8)
                submit(selected)
                true
            }
        }.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown()
                val pointer = down.id
                fun select(y: Float) {
                    val inset = 12.dp.toPx()
                    selected = (8 - 16 * ((y - inset) / (size.height - 2 * inset)).coerceIn(0f, 1f)).roundToInt()
                }
                select(down.position.y)
                down.consume()
                var released = false
                while (!released) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointer } ?: break
                    if (change.isConsumed) break
                    select(change.position.y)
                    released = !change.pressed
                    change.consume()
                }
                if (released) submit(selected)
            }
        }) {
            val inset = 12.dp.toPx()
            val x = size.width / 2
            val length = size.height - inset * 2
            drawLine(Color.Gray, Offset(x, inset), Offset(x, size.height - inset), 4.dp.toPx())
            for (step in -8..8) {
                val y = inset + (8 - step) / 16f * length
                drawLine(if (step == 0) Color.White else Color.Gray, Offset(x - 8.dp.toPx(), y), Offset(x + 8.dp.toPx(), y), 2.dp.toPx())
            }
            drawCircle(if (enabled) Color(0xFF80CBC4) else Color.Gray, 10.dp.toPx(), Offset(x, inset + (8 - selected) / 16f * length))
        }
        Column(Modifier.width(180.dp).padding(start = 8.dp)) {
            Text(if (selected == 0) "= Igualdade" else "${if (selected > 0) "A" else "B"} • intensidade ${abs(selected) + 1}", color = Color.White)
            Text("Toque ou arraste e solte para salvar e avançar.", color = Color.LightGray)
        }
    }
}

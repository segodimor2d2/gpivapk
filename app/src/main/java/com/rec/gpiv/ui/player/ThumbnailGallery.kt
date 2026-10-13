package com.rec.gpiv.ui.player

import android.graphics.BitmapFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.selected
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
fun ThumbnailGallery(state: PlayerUiState, onDismiss: () -> Unit, onCompare: (Int) -> Unit, onFinish: () -> Unit, onToggleSelection: (android.net.Uri) -> Unit,
    onStartAhp: () -> Unit, onRenameSelected: () -> Unit) {
    val selectedEntries = com.rec.gpiv.model.OrderedSelection.entries(state.thumbnails, state.thumbnailSelection, ThumbnailEntry::uri)
    val pair = if (state.ahpReady && state.ahpActive) AhpRanking.pair(selectedEntries.size, state.ahpValues.size) else null
    val total = selectedEntries.size.toLong() * (selectedEntries.size - 1) / 2
    Dialog(onDismissRequest = { if (!state.ahpBusy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false,
        dismissOnBackPress = !state.ahpBusy, dismissOnClickOutside = !state.ahpBusy)) {
        Column(Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.94f).background(Color.Black).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (state.ahpActive) "AHP • ${state.ahpValues.size} / $total" else "Miniaturas • ${state.thumbnailSelection.size} selecionados", color = Color.White, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss, enabled = !state.ahpBusy) { Text("Fechar") }
            }
            state.ahpMessage?.let { Text(it, color = Color.White) }
            if (!state.ahpActive && (state.ahpReady || state.thumbnails.isNotEmpty())) {
                Text("Toque para selecionar na ordem desejada. Toque novamente para remover.", color = Color.LightGray)
                LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.thumbnails, key = { it.uri.toString() }) { entry ->
                        SelectionThumbnail(entry, state.thumbnailSelection.indexOf(entry.uri) + 1,
                            !state.ahpBusy, { onToggleSelection(entry.uri) })
                    }
                }
                val enabled = state.ahpReady && !state.ahpBusy && selectedEntries.isNotEmpty()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = onStartAhp, enabled = enabled) { Text("Aplicar AHP") }
                    TextButton(onClick = onRenameSelected, enabled = enabled) { Text("Renomear na ordem") }
                }
            } else if (pair != null) {
                Text("Qual arquivo você prefere?", color = Color.LightGray)
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Preview(selectedEntries[pair.first], "A", Modifier.weight(1f).fillMaxHeight())
                    Preview(selectedEntries[pair.second], "B", Modifier.weight(1f).fillMaxHeight())
                }
                key(state.ahpValues.size) {
                    PreferenceSlider(!state.ahpBusy, onCompare)
                }
            } else {
                Spacer(Modifier.weight(1f))
                if (state.ahpReady && state.ahpActive && selectedEntries.isNotEmpty()) {
                    TextButton(onClick = onFinish, enabled = !state.ahpBusy) {
                        Text(if (selectedEntries.size == 1) "Renomear único arquivo" else "Tentar renomear novamente")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionThumbnail(entry: ThumbnailEntry, number: Int, enabled: Boolean, onClick: () -> Unit) {
    val preview by produceState<ImageBitmap?>(null, entry.thumbnailPath) {
        value = withContext(Dispatchers.IO) { entry.thumbnailPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() } }
    }
    val thumbnailShape = RoundedCornerShape(12.dp)
    Column(Modifier.semantics { selected = number > 0 }.clickable(enabled = enabled, onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(thumbnailShape).background(Color(0xFF151515))
            .border(if (number > 0) 2.dp else 0.dp, if (number > 0) Color.White else Color.Transparent, thumbnailShape),
            contentAlignment = Alignment.Center) {
            preview?.let { Image(it, entry.name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                ?: Text("Prévia indisponível", color = Color.LightGray, fontSize = 11.sp)
            if (number > 0) {
                Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(34.dp)
                    .background(Color.Black, CircleShape), contentAlignment = Alignment.Center) {
                    Text(number.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
        Text(entry.name, color = Color.White, maxLines = 2, fontSize = 11.sp)
    }
}

@Composable
private fun Preview(entry: ThumbnailEntry, label: String, modifier: Modifier) {
    val preview by produceState<ImageBitmap?>(null, entry.thumbnailPath) {
        value = withContext(Dispatchers.IO) { entry.thumbnailPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() } }
    }
    Column(modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(0.6f).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("A 9", color = Color.White)
            Text("=", color = Color.White)
            Text("B 9", color = Color.White)
        }
        Canvas(Modifier.fillMaxWidth(0.6f).height(48.dp).semantics {
            contentDescription = "Preferência A ou B. Esquerda: A. Centro: igualdade. Direita: B. Solte para salvar."
            progressBarRangeInfo = ProgressBarRangeInfo(-selected.toFloat(), -8f..8f, 15)
            if (enabled) setProgress { value ->
                selected = -value.roundToInt().coerceIn(-8, 8)
                submit(selected)
                true
            }
        }.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown()
                val pointer = down.id
                fun select(x: Float) {
                    val inset = 12.dp.toPx()
                    selected = (8 - 16 * ((x - inset) / (size.width - 2 * inset)).coerceIn(0f, 1f)).roundToInt()
                }
                select(down.position.x)
                down.consume()
                var released = false
                while (!released) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointer } ?: break
                    if (change.isConsumed) break
                    select(change.position.x)
                    released = !change.pressed
                    change.consume()
                }
                if (released) submit(selected)
            }
        }) {
            val inset = 12.dp.toPx()
            val y = size.height / 2
            val length = size.width - inset * 2
            drawLine(Color.Gray, Offset(inset, y), Offset(size.width - inset, y), 4.dp.toPx())
            drawCircle(if (enabled) Color(0xFFFFFFFF) else Color.White, 10.dp.toPx(), Offset(inset + (8 - selected) / 16f * length, y))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (selected == 0) "0" else "${if (selected > 0) "A" else "B"} • ${abs(selected) + 1}", color = Color.White)
            Text("toque ou arraste", color = Color.LightGray)
        }
    }
}

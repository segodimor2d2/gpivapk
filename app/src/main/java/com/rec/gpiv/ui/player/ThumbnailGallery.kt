package com.rec.gpiv.ui.player

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rec.gpiv.model.ThumbnailEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ThumbnailGallery(entries: List<ThumbnailEntry>, onDismiss: () -> Unit, onSelect: (Uri) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.9f).background(Color.Black).padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Miniaturas ${entries.count { it.processed }} / ${entries.size}", color = Color.White, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Fechar", color = Color.White) }
            }
            if (entries.isEmpty()) Text("Nenhum arquivo nesta pasta.", color = Color.White)
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries, key = { it.uri.toString() }) { entry ->
                    val preview by produceState<ImageBitmap?>(null, entry.thumbnailPath) {
                        value = withContext(Dispatchers.IO) { entry.thumbnailPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() } }
                    }
                    Column(Modifier.clickable { onSelect(entry.uri) }) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFF151515)), contentAlignment = Alignment.Center) {
                            preview?.let { Image(it, entry.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                                ?: Text(if (entry.processed) "Sem prévia" else "…", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Text(entry.name, color = Color.White, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

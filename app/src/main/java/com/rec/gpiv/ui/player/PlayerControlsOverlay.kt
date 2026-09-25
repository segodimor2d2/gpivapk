package com.rec.gpiv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rec.gpiv.model.PlayerUiState
import com.rec.gpiv.player.SEEK_FRAMES
import com.rec.gpiv.player.SEEK_FRAMES_PLUS
import com.rec.gpiv.player.SEEK_SECONDS
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity

@Composable
fun PlayerControlsOverlay(
    uiState: PlayerUiState,
    gestureToolState: MutableState<GestureTool>,
    onOpenVideo: () -> Unit,
    onOpenFolder: () -> Unit,
    onPreviousFile: () -> Unit,
    onFirstFile: () -> Unit,
    onLastFile: () -> Unit,
    onJumpFilesBackward: () -> Unit,
    onJumpFilesForward: () -> Unit,
    onNextFile: () -> Unit,
    onFrameBackward: (Int) -> Unit,
    onFrameForward: (Int) -> Unit,
    onSetMarkerA: () -> Unit,
    onSetMarkerB: () -> Unit,
    onTestCut: () -> Unit,
    onSeekBackward: (Double) -> Unit,
    onSeekForward: (Double) -> Unit,
    onDisableABLoop: () -> Unit,
    onSaveFileTag: (String) -> Unit,
    onMakeTags: () -> Unit,
    onTagsEnabledChange: (Boolean) -> Unit,
    onTestMoveTags: () -> Unit,
    onDeleteFile: (Boolean) -> Unit,
    onScreenshot: () -> Unit,
    onSetScreenshotMethod: (String) -> Unit,
    onRotate: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onVolumeDown: () -> Unit,
    onVolumeUp: () -> Unit,
    onMute: () -> Unit,
    onResetVideoAdjustments: () -> Unit
) {
    var gestureTool by gestureToolState
    var controlSubRowA by remember { mutableStateOf(false) }
    var controlSubRowB by remember { mutableStateOf(false) }
    var controlSubRowC by remember { mutableStateOf(false) }
    var controlSubRowD by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }
    val tagFocusRequester = remember { FocusRequester() }
    var deletePopupVisible by remember { mutableStateOf(false) }
    var deletePermanent by remember { mutableStateOf(false) }

    LaunchedEffect(controlSubRowD) {
        if (controlSubRowD) tagFocusRequester.requestFocus()
    }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = LocalTextStyle.current

    val textWidth = remember(texto, textStyle) {
        textMeasurer
            .measure(
                text = texto.ifEmpty { " " },
                style = textStyle
            )
            .size.width
    }

    Box(modifier = Modifier.fillMaxSize()) {
            Column(

                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(
                            alpha = 0.0f
                        )
                    )
                    .navigationBarsPadding()
                    .padding(1.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp,
                            Alignment.CenterHorizontally
                        )
                ) {


                    if (uiState.tagsEnabled) {
                        StatusText(
                            text = "del",
                            modifier = Modifier.clickable {
                                deletePermanent = false
                                deletePopupVisible = true
                            }
                        )
                    }

                    StatusText(
                        text = "${uiState.fileIndex + 1} / ${uiState.fileCount}",
                    )

                    StatusText(
                        text = "${uiState.filename ?: "+ + + +"}",
                        modifier = Modifier
                            .clickable {
                                onOpenVideo()
                            }
                    )

                    StatusText(
                        text = "&&&",
                        modifier =
                            Modifier.clickable {
                                onOpenFolder()
                            }
                    )

                    StatusText(
                        text = "${uiState.fileTag}",
                    )


                }



                if (controlSubRowD) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = texto,
                            onValueChange = { texto = it },
                            singleLine = true,

                            textStyle = textStyle.copy(
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            ),

                            modifier = Modifier
                                .width(
                                    with(density) {
                                        (textWidth + 40.dp.toPx()).toDp()
                                    }
                                )
                                .background(
                                    Color.Black.copy(alpha = 0.6f)
                                )
                                .focusRequester(tagFocusRequester),

                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),

                            keyboardActions = KeyboardActions(
                                onDone = {
                                    onSaveFileTag(texto)
                                    texto = ""
                                    controlSubRowD = false
                                }
                            ),

                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                errorBorderColor = Color.Transparent
                            )
                        )
                    }
                }

            }


            if (controlSubRowD) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {



                    FlowRow(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth(0.8f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.tags.forEach { tag ->
                            Text(
                                text = tag,
                                modifier = Modifier
                                    .background(
                                        Color.Black.copy(alpha = 0.6f)
                                    )
                                    .clickable {
                                        texto = tag
                                        onSaveFileTag(tag)
                                        texto = ""
                                        controlSubRowD = false
                                    }
                                    .padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        /*
         * ----------------------------------------------------
         * POPUP DE EXCLUSÃO
         * ----------------------------------------------------
         */

        if (deletePopupVisible) {
            AlertDialog(
                onDismissRequest = {
                    deletePopupVisible = false
                },
                title = {
                    Text("Excluir arquivo")
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = deletePermanent,
                            onCheckedChange = {
                                deletePermanent = it
                            }
                        )

                        Text(
                            text = "Excluir permanentemente"
                        )
                    }
                },

                confirmButton = {
                    TextButton(
                        onClick = {
                            deletePopupVisible = false
                            onDeleteFile(deletePermanent)
                        }
                    ) {
                        Text("EXCLUIR")
                    }
                },

                dismissButton = {
                    TextButton(
                        onClick = {
                            deletePopupVisible = false
                        }
                    ) {
                        Text("CANCELAR")
                    }
                }
            )
        }

        /*
         * ----------------------------------------------------
         * PAINEL DE CONTROLES
         * ----------------------------------------------------
         */

        Box(modifier = Modifier.fillMaxSize()) {
            Column(

                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(
                            alpha = 0.0f
                        )
                    )
                    .navigationBarsPadding()
                    .padding(1.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {


                if (controlSubRowC) {

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                                // .padding(vertical = 10.dp),

                            horizontalArrangement = Arrangement.Center,

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                              StatusText(
                                  text = "${uiState.fileIndex + 1} / ${uiState.fileCount}",
                                  backgroundColor =
                                      if (uiState.tagsEnabled) {
                                          Color(0x9900994C)
                                      } else {
                                          Color.Transparent
                                      },
                                  modifier = Modifier.clickable {
                                      onTagsEnabledChange(
                                          !uiState.tagsEnabled
                                      )
                                  }
                              )

                              StatusText(
                                  text = "${uiState.filename ?: "+ + + +"}",
                                  modifier = Modifier
                                      .clickable {
                                          onOpenVideo()
                                      }
                              )

                              StatusText(
                                  text = "&&&",
                                  modifier =
                                      Modifier.clickable {
                                          onOpenFolder()
                                      }
                              )

                        }

                        if (uiState.tagsEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                    // .padding(vertical = 10.dp),

                                horizontalArrangement = Arrangement.Center,

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {


                                  StatusText(
                                      text = "mv",
                                      modifier = Modifier
                                          .clickable {
                                              onTestMoveTags()
                                          }
                                  )

                                  StatusText(
                                      text = "mkd",
                                      modifier = Modifier
                                          .clickable {
                                              onMakeTags()
                                          }

                                  )

                                  StatusText(
                                      text = "+tg",
                                      backgroundColor =
                                          if (controlSubRowD) {
                                              Color(0x9900994C)
                                          } else {
                                              Color.Transparent
                                          },
                                      modifier = Modifier.clickable {
                                          controlSubRowD = !controlSubRowD
                                      }
                                  )
                              }
                        }


                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp,
                                    Alignment.CenterHorizontally
                                )
                        ) {

                            CompactButton(
                                text = "+10",
                                onClick = onJumpFilesForward
                            )

                            CompactButton(
                                text = "-10",
                                onClick = onJumpFilesBackward
                            )

                            CompactButton(
                                text = "A",
                                onClick = onFirstFile
                            )

                            CompactButton(
                                text = "Z",
                                onClick = onLastFile
                            )
                        }


                    }
                }

                if (controlSubRowB) {

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy( 10.dp,
                                    Alignment.CenterHorizontally
                                )
                        ) {

                            CompactButton(
                                text = "SS MPV",
                                onClick = {
                                  onSetScreenshotMethod("MPV")
                                  onScreenshot()
                                }
                            )

                            CompactButton(
                                text = "[SS]",
                                onClick = {
                                  onSetScreenshotMethod("FRAMEBUFFER")
                                  onScreenshot()
                                }
                            )

                            CompactButton(
                                text = "[CUT]",
                                onClick = onTestCut
                            )

                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp,
                                    Alignment.CenterHorizontally
                                ),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            StatusText(
                                text = "f${uiState.currentFrame}"
                            )

                            StatusText(
                                text =
                                    uiState.previousKeyframeFrame?.let {
                                        keyframe ->
                                        "KF f$keyframe " +
                                            "(-${uiState.currentFrame - keyframe})"
                                    } ?: "KF --"
                            )

                            CompactButton(
                                text = "A",
                                containerColor =
                                    if (uiState.markerASet) {
                                        Color(0x9900994C)
                                    } else {
                                        Color.Black.copy(alpha = 0.0f)
                                    },
                                onClick = onSetMarkerA
                            )

                            CompactButton(
                                text = "B",
                                containerColor =
                                    if (uiState.markerBSet) {
                                        Color(0x9900994C)
                                    } else {
                                        Color.Black.copy(alpha = 0.0f)
                                    },
                                onClick = onSetMarkerB
                            )

                            CompactButton(
                                text = "0",
                                onClick = onDisableABLoop
                            )

                        }



                    }
                }


                if (controlSubRowA) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                10.dp,
                                Alignment.CenterHorizontally
                            )
                    ) {


                        CompactButton(
                            text = "vol ${uiState.volume}",
                            onClick = onMute
                        )

                        CompactButton(
                            text = "-",
                            onClick = onVolumeDown
                        )


                        CompactButton(
                            text = "+",
                            onClick = onVolumeUp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                10.dp,
                                Alignment.CenterHorizontally
                            )
                    ) {

                        StatusText(
                            text = "Z",
                            backgroundColor =
                                if (gestureTool == GestureTool.ZOOM) {
                                    Color(0x9900994C)
                                } else {
                                    Color.Transparent
                                },
                            modifier = Modifier
                                .clickable {
                                    gestureTool =
                                        if (gestureTool == GestureTool.ZOOM) {
                                            GestureTool.NONE
                                        } else {
                                            GestureTool.ZOOM
                                        }
                                }
                        )

                        StatusText(
                            text = "B",
                            backgroundColor =
                                if (gestureTool == GestureTool.BRIGHTNESS) {
                                    Color(0x9900994C)
                                } else {
                                    Color.Transparent
                                },
                            modifier = Modifier
                                .clickable {
                                    gestureTool =
                                        if (gestureTool == GestureTool.BRIGHTNESS) {
                                            GestureTool.NONE
                                        } else {
                                            GestureTool.BRIGHTNESS
                                        }
                                }
                        )


                        StatusText(
                            text = "C",
                            backgroundColor =
                                if (gestureTool == GestureTool.CONTRAST) {
                                    Color(0x9900994C)
                                } else {
                                    Color.Transparent
                                },
                            modifier = Modifier
                                .clickable {
                                    gestureTool =
                                        if (gestureTool == GestureTool.CONTRAST) {
                                            GestureTool.NONE
                                        } else {
                                            GestureTool.CONTRAST
                                        }
                                }
                        )

                        StatusText(
                            text = "G",
                            backgroundColor =
                                if (gestureTool == GestureTool.GAMMA) {
                                    Color(0x9900994C)
                                } else {
                                    Color.Transparent
                                },
                            modifier = Modifier
                                .clickable {
                                    gestureTool =
                                        if (gestureTool == GestureTool.GAMMA) {
                                            GestureTool.NONE
                                        } else {
                                            GestureTool.GAMMA
                                        }
                                }
                        )

                        StatusText(
                            text = "S",
                            backgroundColor =
                                if (gestureTool == GestureTool.SATURATION) {
                                    Color(0x9900994C)
                                } else {
                                    Color.Transparent
                                },
                            modifier = Modifier
                                .clickable {
                                    gestureTool =
                                        if (gestureTool == GestureTool.SATURATION) {
                                            GestureTool.NONE
                                        } else {
                                            GestureTool.SATURATION
                                        }
                                }
                        )


                        StatusText(
                            text = "0",
                            modifier = Modifier
                                .clickable {
                                    gestureTool = GestureTool.NONE
                                }
                        )
                    }
                }




                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                10.dp,
                                Alignment.CenterHorizontally
                            )
                    ) {


                        CompactButton(
                            text = "+",
                            onClick = onNextFile
                        )

                        CompactButton(
                            text = "-",
                            onClick = onPreviousFile
                        )


                    }


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                10.dp,
                                Alignment.CenterHorizontally
                            )
                    ) {


                        CompactButton(
                            text = "<<",
                            onClick = {
                                onSeekBackward(SEEK_SECONDS)
                            }
                        )

                        CompactButton(
                            text = ">>",
                            onClick = {
                                onSeekForward(SEEK_SECONDS)
                            }
                        )

                        CompactButton(
                            text = "<=",
                            onClick = {
                                onFrameBackward(SEEK_FRAMES_PLUS)
                            }
                        )

                        CompactButton(
                            text = "=>",
                            onClick = {
                                onFrameForward(SEEK_FRAMES_PLUS)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy( 10.dp,
                                Alignment.CenterHorizontally
                            )
                    ) {

                        StatusText(
                            text = "↻",
                            modifier =
                                Modifier.clickable {
                                    onRotate()
                                }
                        )
                        CompactButton(
                            text = "<",
                            onClick = {
                                onFrameBackward(SEEK_FRAMES)
                            }
                        )

                        CompactButton(
                            text = ">",
                            onClick = {
                                onFrameForward(SEEK_FRAMES)
                            }
                        )
                        StatusText(
                            text = "Z",
                            backgroundColor =
                                if (gestureTool == GestureTool.ZOOM) {
                                    Color(0x9900994C)
                                } else {
                                    Color.Transparent
                                },
                            modifier = Modifier
                                .clickable {
                                    gestureTool =
                                        if (gestureTool == GestureTool.ZOOM) {
                                            GestureTool.NONE
                                        } else {
                                            GestureTool.ZOOM
                                        }
                                }
                        )

                    }


                }


                /*
                 * --------------------------------------------
                 * STATUS / INFO
                 * --------------------------------------------
                        // modifier = Modifier.clickable { controlsVisible = false }
                 */


                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy( 10.dp, Alignment.CenterHorizontally),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {



                    StatusText(
                        text =
                            "%.2f".format(
                                uiState.position
                            ),
                        modifier = Modifier.clickable { onResetVideoAdjustments() }
                    )


                    StatusText(
                        text =
                            "%.2f".format(
                                uiState.duration
                            ),
                        backgroundColor =
                            if (controlSubRowB) {
                                Color(0x9900994C)
                            } else {
                                Color.Transparent
                            },
                        modifier = Modifier.clickable {
                            controlSubRowB = !controlSubRowB
                        }
                    )

                    StatusText(
                        text =
                            if (uiState.duration > 0.0) {
                                "%.1f%%".format(
                                    (uiState.position /
                                        uiState.duration) * 100.0
                                )
                            } else {
                                "0.0%"
                            },

                        backgroundColor =
                            if (uiState.playing) {
                                Color(0x9900994C)
                            } else {
                                Color.Transparent
                            },

                        modifier = Modifier.clickable {
                            onTogglePlayPause()
                        }
                    )


                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),

                    horizontalArrangement = Arrangement.Center,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {



                    StatusText(
                        text = "ff",
                        backgroundColor =
                            if (controlSubRowA) {
                                Color(0x9900994C)
                            } else {
                                Color.Transparent
                            },
                        modifier = Modifier.clickable {
                            controlSubRowA = !controlSubRowA
                        }
                    )

                    StatusText(
                        text = "%",
                        backgroundColor =
                            if (gestureTool == GestureTool.SEEK_PERCENT) {
                                Color(0x9900994C)
                            } else {
                                Color.Transparent
                            },
                        modifier = Modifier
                            .clickable {
                                gestureTool =
                                    if (gestureTool == GestureTool.SEEK_PERCENT) {
                                        GestureTool.NONE
                                    } else {
                                        GestureTool.SEEK_PERCENT
                                    }
                            }
                    )

                    StatusText(
                        text = "pp",
                        backgroundColor =
                            if (gestureTool == GestureTool.SEEK) {
                                Color(0x9900994C)
                            } else {
                                Color.Transparent
                            },
                        modifier = Modifier
                            .clickable {
                                gestureTool =
                                    if (gestureTool == GestureTool.SEEK) {
                                        GestureTool.NONE
                                    } else {
                                        GestureTool.SEEK
                                    }
                            }
                    )

                    StatusText(
                        text = "()",
                        backgroundColor =
                            if (controlSubRowC) {
                                Color(0x9900994C)
                            } else {
                                Color.Transparent
                            },
                        modifier = Modifier.clickable {
                            controlSubRowC = !controlSubRowC
                        }
                    )


                }


            }
        }
    }
// ${uiState.fileIndex + 1} / ${uiState.fileCount}

/*
 * ============================================================
 * BOTÃO COMPACTO
 * ============================================================
 */

@Composable
private fun CompactButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color = Color.Black.copy(alpha = 0.0f),
    modifier: Modifier = Modifier
) {

    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize
            provides 0.dp
    ) {

        Button(
            onClick = onClick,

            modifier = modifier
                .height(28.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor =
                    Color.White
            ),

            contentPadding =
                PaddingValues(
                    horizontal = 8.dp,
                    vertical = 0.dp
                )
        ) {

            Text(
                text = text,
                fontSize = 16.sp,

                style = LocalTextStyle.current.copy(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(0f, 0f),
                        blurRadius = 3f
                    )
                )

            )
        }
    }
}

@Composable
private fun StatusText(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black.copy(alpha = 0.0f)
) {

    Text(
        text = text,
        fontSize = 16.sp,
        color = Color.White,
        maxLines = 1,

        style = LocalTextStyle.current.copy(
            shadow = Shadow(
                color = Color.Black,
                offset = Offset(0f, 0f),
                blurRadius = 3f
            )
        ),

        modifier = modifier
            .background(backgroundColor)
            .padding(
                horizontal = 8.dp,
        vertical = 5.dp
            )
    )
}

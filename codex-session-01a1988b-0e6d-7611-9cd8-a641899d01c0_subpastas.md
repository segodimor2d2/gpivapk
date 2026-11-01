# Codex conversation

## User

quero que ao usar o botão "@" ele compartilhe o arquivo atual

## Assistant

Vou localizar o botão “@” e ajustar a ação para compartilhar o arquivo que estiver aberto.

## Activity

    $ pwd; rg --files -g 'AGENTS.md' -g '*.kt' -g '*.java' -g '*.xml' | head -70
    /home/segodimo/05android/gpivapk/gpivapk
    app/src/main/AndroidManifest.xml
    app/src/main/res/xml/backup_rules.xml
    app/src/main/res/xml/data_extraction_rules.xml
    app/src/main/res/values/strings.xml
    app/src/main/res/values-v31/themes.xml
    app/src/main/res/values/themes.xml
    app/src/main/res/values/colors.xml
    app/src/main/res/drawable/splash_empty.xml
    app/src/main/res/drawable/ic_launcher_foreground.xml
    app/src/main/res/drawable/ic_launcher_background.xml
    app/src/main/res/drawable/file_browser_background.xml
    app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
    app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
    app/src/main/java/com/rec/gpiv/MainActivity.kt
    app/src/androidTest/java/com/rec/gpiv/ExampleInstrumentedTest.kt
    app/src/main/java/com/rec/gpiv/data/AhpRepository.kt
    app/src/main/java/com/rec/gpiv/data/FolderRepository.kt
    app/src/main/java/com/rec/gpiv/data/ThumbnailRepository.kt
    app/src/main/java/com/rec/gpiv/player/MpvNative.kt
    app/src/main/java/com/rec/gpiv/player/PlayerConstants.kt
    app/src/main/java/com/rec/gpiv/player/FileList.kt
    app/src/main/java/com/rec/gpiv/player/VideoSource.kt
    app/src/main/java/com/rec/gpiv/player/PlayerEvent.kt
    app/src/main/java/com/rec/gpiv/player/FileItem.kt
    app/src/main/java/com/rec/gpiv/player/MpvPlayer.kt
    app/src/main/java/com/rec/gpiv/player/FileNameOrder.kt
    app/src/main/java/com/rec/gpiv/player/FakeVideoPlayer.kt
    app/src/main/java/com/rec/gpiv/player/VideoPlayer.kt
    app/src/main/java/com/rec/gpiv/ui/theme/Theme.kt
    app/src/main/java/com/rec/gpiv/ahp/AhpRanking.kt
    app/src/main/java/com/rec/gpiv/ui/theme/Color.kt
    app/src/main/java/com/rec/gpiv/ui/browser/TerminalFileBrowser.kt
    app/src/main/java/com/rec/gpiv/ui/theme/Type.kt
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerMarkerController.kt
    app/src/main/java/com/rec/gpiv/viewmodel/MediaExportManager.kt
    app/src/main/java/com/rec/gpiv/viewmodel/CutFrameInfo.kt
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerCutController.kt
    app/src/main/java/com/rec/gpiv/viewmodel/FileTrashManager.kt
    app/src/main/java/com/rec/gpiv/viewmodel/TagFileManager.kt
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerControls.kt
    app/src/main/java/com/rec/gpiv/ui/common/BlackNotice.kt
    app/src/main/java/com/rec/gpiv/viewmodel/TagCsv.kt
    app/src/main/java/com/rec/gpiv/ui/player/GestureTool.kt
    app/src/main/java/com/rec/gpiv/ui/player/CropSelectionOverlay.kt
    app/src/main/java/com/rec/gpiv/ui/player/PlayerScreen.kt
    app/src/main/java/com/rec/gpiv/ui/player/ThumbnailGallery.kt
    app/src/main/java/com/rec/gpiv/model/CropRegion.kt
    app/src/main/java/com/rec/gpiv/ui/player/PlayerSurface.kt
    app/src/main/java/com/rec/gpiv/model/PlayerUiState.kt
    app/src/main/java/com/rec/gpiv/ui/player/PlayerControlsOverlay.kt
    app/src/main/java/com/rec/gpiv/ui/player/PlayerGestures.kt
    app/src/main/java/com/rec/gpiv/model/OrderedSelection.kt
    app/src/test/java/com/rec/gpiv/ExampleUnitTest.kt
    app/src/test/java/com/rec/gpiv/FileNameOrderTest.kt
    app/src/test/java/com/rec/gpiv/AhpRankingTest.kt
    app/src/test/java/com/rec/gpiv/OrderedSelectionTest.kt
    app/src/test/java/com/rec/gpiv/TagCsvTest.kt
    ✓ • 0ms

## Activity

    $ git status --short; rg -n '"@"|text="@"|share|ACTION_SEND' app/src/main
     M app/src/main/java/com/rec/gpiv/ui/player/PlayerControlsOverlay.kt
    app/src/main/res/xml/backup_rules.xml:10:   <include domain="sharedpref" path="."/>
    app/src/main/res/xml/backup_rules.xml:11:   <exclude domain="sharedpref" path="device.xml"/>
    app/src/main/java/com/rec/gpiv/ui/player/PlayerControlsOverlay.kt:208:                        text = "@",
    ✓ • 0ms

## Activity

    $ find .. -name AGENTS.md -print; cat app/src/main/AndroidManifest.xml; cat app/src/main/java/com/rec/gpiv/player/VideoSource.kt; cat app/src/main/java/com/rec/gpiv/ui/player/PlayerScreen.kt
    <?xml version="1.0" encoding="utf-8"?>
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:tools="http://schemas.android.com/tools">
    
        <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage" />
        <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="29" />
        <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29" />
    
        <application
            android:allowBackup="true"
            android:requestLegacyExternalStorage="true"
            android:dataExtractionRules="@xml/data_extraction_rules"
            android:fullBackupContent="@xml/backup_rules"
            android:icon="@mipmap/ic_launcher"
            android:label="@string/app_name"
            android:roundIcon="@mipmap/ic_launcher_round"
            android:supportsRtl="true"
            android:theme="@style/Theme.Gpiv">
            <activity
                android:name=".MainActivity"
                android:exported="true"
                android:label="@string/app_name"
                android:theme="@style/Theme.Gpiv"
                android:windowSoftInputMode="adjustResize">
    
                <!-- Abrir o GPIV normalmente -->
                <intent-filter>
                    <action android:name="android.intent.action.MAIN" />
                    <category android:name="android.intent.category.LAUNCHER" />
                </intent-filter>
    
                <!-- Receber arquivos de outros aplicativos -->
                <intent-filter>
                    <action android:name="com.rec.gpiv.action.OPEN_FOLDER" />
                    <category android:name="android.intent.category.DEFAULT" />
                    <data android:scheme="file" />
                    <data android:scheme="content" />
                </intent-filter>
    
                <intent-filter>
                    <action android:name="com.rec.gpiv.action.OPEN_FOLDER" />
                    <category android:name="android.intent.category.DEFAULT" />
                    <data android:scheme="file" />
                    <data android:scheme="content" />
                    <data android:mimeType="*/*" />
                </intent-filter>
    
                <intent-filter>
                    <action android:name="android.intent.action.VIEW" />
                    <category android:name="android.intent.category.DEFAULT" />
                    <data android:scheme="file" />
                    <data android:scheme="content" />
                    <data android:mimeType="vnd.android.document/directory" />
                    <data android:mimeType="resource/folder" />
                    <data android:mimeType="inode/directory" />
                </intent-filter>
    
                <intent-filter>
                    <action android:name="android.intent.action.VIEW" />
    
                    <category android:name="android.intent.category.DEFAULT" />
    
                    <data android:mimeType="*/*" />
                    <data android:mimeType="video/*" />
                    <data android:mimeType="image/*" />
                    <data android:mimeType="audio/*" />
    
                    <data android:mimeType="application/octet-stream" />
                </intent-filter>
    
            </activity>
        </application>
    
    </manifest>
    package com.rec.gpiv.player
    
    import android.content.ContentResolver
    import android.net.Uri
    import android.os.ParcelFileDescriptor
    
    class VideoSource(
        private val contentResolver: ContentResolver
    ) {
    
        private var parcelFileDescriptor: ParcelFileDescriptor? = null
    
        fun open(uri: Uri): Boolean {
    
            close()
    
            println(
                "VideoSource: abrindo URI = $uri"
            )
    
            parcelFileDescriptor =
                contentResolver.openFileDescriptor(
                    uri,
                    "r"
                )
    
            println(
                "VideoSource: PFD = $parcelFileDescriptor"
            )
    
            println(
                "VideoSource: FD = ${parcelFileDescriptor?.fd}"
            )
    
            return parcelFileDescriptor != null
        }
    
        fun getFileDescriptor(): Int? {
            return parcelFileDescriptor?.fd
        }
    
        fun openIndependentFileDescriptor(uri: Uri): ParcelFileDescriptor? {
    
            println(
                "VideoSource: abrindo segundo PFD = $uri"
            )
    
            val pfd =
                contentResolver.openFileDescriptor(
                    uri,
                    "r"
                )
    
            println(
                "VideoSource: segundo PFD = $pfd"
            )
    
            println(
                "VideoSource: segundo FD = ${pfd?.fd}"
            )
    
            return pfd
        }
    
        fun close() {
    
            val fd = parcelFileDescriptor?.fd
    
            if (fd != null) {
                println(
                    "VideoSource: closing fd = $fd"
                )
            }
    
            parcelFileDescriptor?.close()
            parcelFileDescriptor = null
        }
    }
    package com.rec.gpiv.ui.player
    
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.background
    import androidx.compose.ui.graphics.Color
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.setValue
    import androidx.compose.ui.Modifier
    import com.rec.gpiv.model.CropRegion
    import com.rec.gpiv.model.PlayerUiState
    import com.rec.gpiv.player.MpvNative
    
    
    @Composable
    fun PlayerScreen(
        uiState: PlayerUiState,
        mpvNative: MpvNative,
    
        onOpenVideo: () -> Unit,
        onOpenFolder: () -> Unit,
        onRefreshFiles: () -> Unit,
        onOpenThumbnails: () -> Unit,
        onCloseThumbnails: () -> Unit,
        onCompareThumbnails: (Int) -> Unit,
        onFinishAhp: () -> Unit,
        onToggleThumbnailSelection: (android.net.Uri) -> Unit,
        onStartSelectedAhp: () -> Unit,
        onRenameSelectedThumbnails: () -> Unit,
    
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
        onTestCut: (CropRegion?) -> Unit,
        onTestCopyCut: () -> Unit,
        onSeekBackward: (Double) -> Unit,
        onSeekForward: (Double) -> Unit,
        onSeekTo: (Double) -> Unit,
        onDisableABLoop: () -> Unit,
        onSaveFileTag: (String) -> Unit,
        onMakeTags: () -> Unit,
        onTagsEnabledChange: (Boolean) -> Unit,
        onTestMoveTags: () -> Unit,
        onDeleteFile: (Boolean) -> Unit,
    
        onScreenshot: (CropRegion?) -> Unit,
        onSetScreenshotMethod: (String) -> Unit,
    
        onRotate: () -> Unit,
    
        onTogglePlayPause: () -> Unit,
        onVolumeDown: () -> Unit,
        onVolumeUp: () -> Unit,
        onMute: () -> Unit,
    
        onResetVideoAdjustments: () -> Unit,
    
        onSurfaceReady: () -> Unit,
    
        modifier: Modifier = Modifier
    ) {
    
        /*
         * --------------------------------------------------------
         * CONTROLES
         * --------------------------------------------------------
         *
         * false = painel escondido
         * true  = painel visível
         */
    
        var crop by remember(uiState.filename, uiState.fileIndex) { mutableStateOf<CropRegion?>(null) }
    
        var controlsVisible by remember {
            mutableStateOf(true)
        }
    
        val gestureToolState = remember {
            mutableStateOf(GestureTool.NONE)
        }
    
        fun selectFilter(tool: GestureTool) {
            gestureToolState.value = if (gestureToolState.value == tool) GestureTool.NONE else tool
        }
    
        /*
         * --------------------------------------------------------
         * TELA INTEIRA
         * --------------------------------------------------------
         *
         * O vídeo ocupa toda a área.
         *
         * Os controles ficam sobrepostos ao vídeo.
         */
    
        Box(
            modifier = modifier.fillMaxSize().background(Color.Black)
        ) {
    
            /*
             * ----------------------------------------------------
             * VÍDEO
             * ----------------------------------------------------
             */
    
            PlayerSurface(
                mpvNative = mpvNative,
                onSurfaceReady = onSurfaceReady,
                modifier = Modifier.fillMaxSize()
            )
    
    
            /*
             * ----------------------------------------------------
             * ZOOM/PAN
             * ----------------------------------------------------
             */
            PlayerGestures(
                mpvNative = mpvNative,
                gestureTool = gestureToolState.value,
                onDoubleTap = {
                    // nextControlRow()
                },
    
                onTwoFingerTap = {
                    controlsVisible = !controlsVisible
                },
    
                onSeekBackward = onSeekBackward,
                onSeekForward = onSeekForward,
                position = uiState.position,
                duration = uiState.duration,
                onSeekTo = onSeekTo,
            )
    
            if (uiState.filename == null) {
                Box(Modifier.fillMaxSize().background(Color.Black))
            }
    
            crop?.let { region ->
                CropSelectionOverlay(region = region, onChange = { crop = it })
            }
    
            if (controlsVisible) {
                PlayerControlsOverlay(
                    uiState = uiState,
                    gestureToolState = gestureToolState,
                    onOpenVideo = onOpenVideo,
                    onOpenFolder = onOpenFolder,
                    onRefreshFiles = onRefreshFiles,
                    onOpenThumbnails = onOpenThumbnails,
                    onPreviousFile = onPreviousFile,
                    onFirstFile = onFirstFile,
                    onLastFile = onLastFile,
                    onJumpFilesBackward = onJumpFilesBackward,
                    onJumpFilesForward = onJumpFilesForward,
                    onNextFile = onNextFile,
                    onFrameBackward = onFrameBackward,
                    onFrameForward = onFrameForward,
                    onSetMarkerA = onSetMarkerA,
                    onSetMarkerB = onSetMarkerB,
                    onTestCut = { onTestCut(crop) },
                    onTestCopyCut = onTestCopyCut,
                    onSeekBackward = onSeekBackward,
                    onSeekForward = onSeekForward,
                    onDisableABLoop = onDisableABLoop,
                    onSaveFileTag = onSaveFileTag,
                    onMakeTags = onMakeTags,
                    onTagsEnabledChange = onTagsEnabledChange,
                    onTestMoveTags = onTestMoveTags,
                    onDeleteFile = onDeleteFile,
                    onScreenshot = { onScreenshot(crop) },
                    onSetScreenshotMethod = onSetScreenshotMethod,
                    onRotate = onRotate,
                    onTogglePlayPause = onTogglePlayPause,
                    onVolumeDown = onVolumeDown,
                    onVolumeUp = onVolumeUp,
                    onMute = onMute,
                    onToggleNegative = mpvNative::toggleNegative,
                    onToggleVerticalMirror = mpvNative::toggleVerticalMirror,
                    onToggleHorizontalMirror = mpvNative::toggleHorizontalMirror,
                    onToggleNoiseReduction = {
                        selectFilter(GestureTool.NOISE_REDUCTION)
                    },
                    onToggleSharpness = {
                        selectFilter(GestureTool.SHARPNESS)
                    },
                    onToggleBlur = {
                        selectFilter(GestureTool.BLUR)
                    },
                    onToggleDeband = {
                        selectFilter(GestureTool.DEBAND)
                    },
                    onToggleEdgeDetection = {
                        selectFilter(GestureTool.EDGE_DETECTION)
                    },
                    cropEnabled = crop != null,
                    onToggleCrop = {
                        crop = if (crop == null) CropRegion(0.2f, 0.2f, 0.8f, 0.7f) else null
                    },
                    onResetVideoAdjustments = {
                        onResetVideoAdjustments()
                        gestureToolState.value = GestureTool.NONE
                    }
                )
            }
            if (uiState.galleryVisible) {
                ThumbnailGallery(uiState, onCloseThumbnails, onCompareThumbnails, onFinishAhp,
                    onToggleThumbnailSelection, onStartSelectedAhp, onRenameSelectedThumbnails)
            }
        }
    }
    ✓ • 48ms

## Activity

    $ sed -n '1,260p' app/src/main/java/com/rec/gpiv/ui/player/PlayerControlsOverlay.kt; cat app/src/main/java/com/rec/gpiv/MainActivity.kt; rg -n 'currentFile|currentSource|Uri|uri' app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt app/src/main/java/com/rec/gpiv/model/PlayerUiState.kt app/src/main/java/com/rec/gpiv/player/FileItem.kt; cat app/build.gradle.kts
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
    import androidx.compose.ui.platform.LocalSoftwareKeyboardController
    import androidx.compose.ui.text.rememberTextMeasurer
    import com.rec.gpiv.model.OperationStatus
    
    @Composable
    fun PlayerControlsOverlay(
        uiState: PlayerUiState,
        gestureToolState: MutableState<GestureTool>,
        onOpenVideo: () -> Unit,
        onOpenFolder: () -> Unit,
        onRefreshFiles: () -> Unit,
        onOpenThumbnails: () -> Unit,
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
        onTestCopyCut: () -> Unit,
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
        onToggleNegative: () -> Unit,
        onToggleVerticalMirror: () -> Unit,
        onToggleHorizontalMirror: () -> Unit,
        onToggleNoiseReduction: () -> Unit,
        onToggleSharpness: () -> Unit,
        onToggleBlur: () -> Unit,
        onToggleDeband: () -> Unit,
        onToggleEdgeDetection: () -> Unit,
        onResetVideoAdjustments: () -> Unit,
        cropEnabled: Boolean,
        onToggleCrop: () -> Unit
    ) {
        var gestureTool by gestureToolState
        var controlSubRowA by remember { mutableStateOf(false) }
        var controlSubRowB by remember { mutableStateOf(false) }
        var controlSubRowC by remember { mutableStateOf(false) }
        var controlSubRowD by remember { mutableStateOf(false) }
        var nextFileAfterTag by remember { mutableStateOf(false) }
        var taggedFileIndex by remember { mutableStateOf<Int?>(null) }
        var texto by remember { mutableStateOf("") }
        val tagFocusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        var ahpConfirmationVisible by remember { mutableStateOf(false) }
        var deletePopupVisible by remember { mutableStateOf(false) }
        var deletePermanent by remember { mutableStateOf(false) }
    
        LaunchedEffect(controlSubRowD) {
            if (!controlSubRowD) keyboardController?.hide()
        }
    
        LaunchedEffect(uiState.fileIndex, uiState.tagsEnabled, nextFileAfterTag, taggedFileIndex) {
            if (!uiState.tagsEnabled || !nextFileAfterTag) {
                taggedFileIndex = null
                if (!uiState.tagsEnabled) controlSubRowD = false
            } else if (taggedFileIndex != null && uiState.fileIndex != taggedFileIndex) {
                taggedFileIndex = null
                controlSubRowD = true
            }
        }
    
        fun saveTag(tag: String) {
            onSaveFileTag(tag)
            texto = ""
            controlSubRowD = false
            if (nextFileAfterTag) {
                taggedFileIndex = if (uiState.fileIndex < uiState.fileCount - 1) {
                    uiState.fileIndex
                } else {
                    null
                }
                onNextFile()
            }
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
    
        if (ahpConfirmationVisible) {
            AlertDialog(
                onDismissRequest = { ahpConfirmationVisible = false },
                title = { Text("Abrir seleção de miniaturas?") },
                text = { Text("Selecione imagens e vídeos na ordem desejada. Depois escolha aplicar o AHP ou renomear diretamente na ordem selecionada. Os arquivos selecionados serão renomeados sem extensão.") },
                confirmButton = {
                    TextButton(onClick = {
                        ahpConfirmationVisible = false
                        onOpenThumbnails()
                    }) { Text("Continuar") }
                },
                dismissButton = {
                    TextButton(onClick = { ahpConfirmationVisible = false }) { Text("Cancelar") }
                }
            )
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
    
    
    
                        StatusText(
                            text = "${uiState.fileIndex + 1} / ${uiState.fileCount}",
                            modifier = Modifier.clickable {
                                deletePermanent = false
                                deletePopupVisible = true
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
                            text = "${uiState.fileTag}",
                            modifier =
                                Modifier.clickable {
                                    onRefreshFiles()
                                }
                        )
    
                        StatusText(
                            text = "@",
                            modifier =
                                Modifier.clickable { }
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
                                    showKeyboardOnFocus = false,
                                    imeAction = ImeAction.Done
                                ),
    
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        saveTag(texto)
                                    }
                                ),
    
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    disabledBorderColor = Color.Transparent,
                                    errorBorderColor = Color.Transparent
    package com.rec.gpiv
    
    import android.Manifest
    import android.app.AlertDialog
    import android.content.ActivityNotFoundException
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.os.Build
    import android.os.Environment
    import android.provider.Settings
    import android.provider.DocumentsContract
    import com.rec.gpiv.ui.common.BlackNotice
    import androidx.core.content.ContextCompat
    import androidx.lifecycle.lifecycleScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import java.io.File
    import android.net.Uri
    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.activity.enableEdgeToEdge
    import androidx.activity.SystemBarStyle
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.activity.viewModels
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.padding
    import androidx.compose.material3.Scaffold
    import androidx.compose.runtime.getValue
    import androidx.compose.ui.Modifier
    import androidx.lifecycle.compose.collectAsStateWithLifecycle
    import com.rec.gpiv.ui.player.PlayerScreen
    import com.rec.gpiv.ui.browser.showTerminalFileBrowser
    import com.rec.gpiv.ui.browser.TerminalFileBrowserDialog
    import com.rec.gpiv.ui.theme.GpivTheme
    import com.rec.gpiv.viewmodel.PlayerViewModel
    
    class MainActivity : ComponentActivity() {
    
        private var notice: BlackNotice? = null
        private var browserDialog: TerminalFileBrowserDialog? = null
    
        private fun showNotice(message: String) {
            if (isFinishing || isDestroyed) return
            val current = notice ?: BlackNotice(findViewById(android.R.id.content)).also { notice = it }
            current.show(message)
        }
    
        override fun onDestroy() {
            browserDialog?.dismiss()
            browserDialog = null
            notice?.dismiss()
            super.onDestroy()
        }
    
        private val viewModel: PlayerViewModel by viewModels()
    
        private val openFolderLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            uri?.let(::persistAndLoadFolder)
        }
    
        private var browserDirectory: File = Environment.getExternalStorageDirectory()
    
        private fun hasStorageAccess(): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
            }
    
        private val storageSettingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (hasStorageAccess()) showStorageBrowser(browserDirectory)
            else showNotice("Ative o acesso a todos os arquivos para navegar pelas pastas.")
        }
    
        private val storagePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            if (hasStorageAccess()) showStorageBrowser(browserDirectory)
            else showNotice("O acesso ao armazenamento não foi autorizado.")
        }
    
        private fun openStorageBrowser() {
            if (hasStorageAccess()) {
                showStorageBrowser(browserDirectory)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                AlertDialog.Builder(this)
                    .setTitle("Acesso a todos os arquivos")
                    .setMessage("Autorize o GPIV na próxima tela para abrir arquivos e navegar entre pastas sem autorizar cada pasta.")
                    .setPositiveButton("Autorizar") { _, _ ->
                        try {
                            storageSettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:$packageName")))
                        } catch (_: ActivityNotFoundException) {
                            storageSettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } else {
                storagePermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE))
            }
        }
    
        private fun showStorageBrowser(directory: File) {
            lifecycleScope.launch {
                val entries = withContext(Dispatchers.IO) {
                    directory.listFiles()?.toList()
                        ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
                }
                if (isFinishing || isDestroyed) return@launch
                if (entries == null) {
                    showNotice("Esta pasta não está acessível.")
                    return@launch
                }
                browserDirectory = directory
                val roots = if (directory == Environment.getExternalStorageDirectory()) {
                    getExternalFilesDirs(null).mapNotNull { it?.absolutePath?.substringBefore("/Android/") }
                        .map(::File).filter { it != directory && it.isDirectory }
                } else emptyList()
                browserDialog = showTerminalFileBrowser(
                    context = this@MainActivity,
                    directory = directory,
                    entries = roots + entries,
                    onOpen = { item ->
                        if (item.isDirectory) showStorageBrowser(item)
                        else viewModel.onLocalFolderSelected(directory, item)
                    },
                    onSelectFolder = { viewModel.onLocalFolderSelected(directory) },
                    onSelectDocumentTree = { openFolderLauncher.launch(null) },
                    existingDialog = browserDialog?.takeIf { it.isShowing }
                )
            }
        }
    
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            )
            handleIncomingIntent(intent)
    
            setContent {
                GpivTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = androidx.compose.ui.graphics.Color.Black
                    ) { innerPadding ->
                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
                        PlayerScreen(
                            uiState = uiState,
                            mpvNative = viewModel.mpvNative,
                            onOpenVideo = {
                                openStorageBrowser()
                            },
                            onOpenFolder = {
                                openStorageBrowser()
                            },
                            onRefreshFiles = viewModel::refreshFiles,
                            onOpenThumbnails = viewModel::openThumbnailGallery,
                            onCloseThumbnails = viewModel::closeThumbnailGallery,
                            onCompareThumbnails = viewModel::compareThumbnails,
                            onFinishAhp = viewModel::finishAhp,
                            onToggleThumbnailSelection = viewModel::toggleThumbnailSelection,
                            onStartSelectedAhp = viewModel::startSelectedAhp,
                            onRenameSelectedThumbnails = viewModel::renameSelectedThumbnails,
                            onPreviousFile = viewModel::previousFile,
                            onFirstFile = viewModel::firstFile,
                            onLastFile = viewModel::lastFile,
                            onJumpFilesBackward = viewModel::jumpFilesBackward,
                            onJumpFilesForward = viewModel::jumpFilesForward,
                            onNextFile = viewModel::nextFile,
                            onFrameBackward = viewModel::frameBackward,
                            onFrameForward = viewModel::frameForward,
                            onSetMarkerA = viewModel::setMarkerA,
                            onSetMarkerB = viewModel::setMarkerB,
                            onSaveFileTag = viewModel::testSaveFileTag,
                            onMakeTags = viewModel::makeTagFolders,
                            onTestMoveTags = viewModel::testMoveTags,
                            onDeleteFile = viewModel::deleteFile,
                            onTagsEnabledChange = viewModel::setTagsEnabled,
                            onTestCut = viewModel::testCurrentCut,
                            onTestCopyCut = viewModel::testCurrentCopyCut,
                            onDisableABLoop = viewModel::disableABLoop,
                            onScreenshot = viewModel::screenshot,
                            onSetScreenshotMethod = viewModel::setScreenshotMethod,
                            onRotate = viewModel.mpvNative::rotateClockwise,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onSeekBackward = viewModel::seekBackward,
                            onSeekForward = viewModel::seekForward,
                            onSeekTo = viewModel::seekTo,
                            onVolumeDown = viewModel::volumeDown,
                            onVolumeUp = viewModel::volumeUp,
                            onMute = viewModel::mute,
                            onResetVideoAdjustments = viewModel::resetVideoAdjustments,
                            onSurfaceReady = viewModel::onSurfaceReady,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    
        override fun onNewIntent(intent: Intent?) {
            super.onNewIntent(intent)
            setIntent(intent)
            handleIncomingIntent(intent)
        }
    
        private fun handleIncomingIntent(intent: Intent?) {
            if (intent == null) return
            val uri = intent.data ?: return
            when (intent.action) {
                ACTION_OPEN_FOLDER -> openIncomingFolder(uri, intent.flags)
                Intent.ACTION_VIEW -> {
                    val isFolder = uri.scheme == "file" && uri.path?.let { File(it).isDirectory } == true ||
                        uri.scheme == "content" && (DocumentsContract.isTreeUri(uri) ||
                            intent.type == DocumentsContract.Document.MIME_TYPE_DIR ||
                            intent.type == "resource/folder" || intent.type == "inode/directory")
                    if (isFolder) openIncomingFolder(uri, intent.flags)
                    else viewModel.handleIncomingUri(uri)
                }
            }
        }
    
        private fun openIncomingFolder(uri: Uri, intentFlags: Int) {
            val localFolder = when {
                uri.scheme == "file" -> uri.path?.let(::File)
                uri.scheme == "content" && uri.authority == "com.android.externalstorage.documents" -> {
                    val id = runCatching {
                        if (uri.pathSegments.contains("document")) DocumentsContract.getDocumentId(uri)
                        else DocumentsContract.getTreeDocumentId(uri)
                    }.getOrNull()
                    id?.let {
                        val volume = it.substringBefore(':')
                        val root = if (volume == "primary") Environment.getExternalStorageDirectory()
                            else File("/storage/$volume")
                        File(root, it.substringAfter(':', ""))
                    }
                }
                else -> null
            }
            if (localFolder?.isDirectory == true && localFolder.canRead()) {
                browserDirectory = localFolder
                viewModel.onLocalFolderSelected(localFolder)
                return
            }
            if (uri.scheme != "content" || !DocumentsContract.isTreeUri(uri)) {
                showNotice("Pasta inacessível. Envie uma URI file:// acessível ou uma URI de árvore SAF com permissão de leitura.")
                return
            }
            val flags = intentFlags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (intentFlags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0 && flags != 0) {
                // Uma concessão temporária também permite abrir a pasta nesta sessão.
                runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
            }
            runCatching {
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    uri, DocumentsContract.getTreeDocumentId(uri)
                )
                requireNotNull(contentResolver.query(childrenUri,
                    arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)) {
                    "Pasta inacessível"
                }.use { /* Verifica o acesso antes de substituir a pasta atual. */ }
                viewModel.onFolderSelected(uri)
            }
                .onFailure { showNotice("Não foi possível abrir a pasta: ${it.message}") }
        }
    
        private fun persistAndLoadFolder(uri: Uri) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    
            runCatching {
                contentResolver.takePersistableUriPermission(uri, flags)
                viewModel.onFolderSelected(uri)
            }.onFailure { error ->
                println("MainActivity: erro ao selecionar pasta: ${error.message}")
            }
        }
    
        companion object {
            const val ACTION_OPEN_FOLDER = "com.rec.gpiv.action.OPEN_FOLDER"
        }
    }
    app/src/main/java/com/rec/gpiv/player/FileItem.kt:3:import android.net.Uri
    app/src/main/java/com/rec/gpiv/player/FileItem.kt:6:    val uri: Uri,
    app/src/main/java/com/rec/gpiv/model/PlayerUiState.kt:5:data class ThumbnailEntry(val uri: android.net.Uri, val name: String, val thumbnailPath: String? = null, val processed: Boolean = false, val mediaType: String? = null, val modifiedTime: Long = 0)
    app/src/main/java/com/rec/gpiv/model/PlayerUiState.kt:15:    val thumbnailSelection: List<android.net.Uri> = emptyList(),
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:3:import android.net.Uri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:69:    private var selectedUri: Uri? = null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:73:    private var pendingUri: Uri? = null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:85:    private var ahpFolder: Uri? = null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:90:        val folder = fileList.getTagFolderUri() ?: return
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:106:                    val type = withContext(Dispatchers.IO) { ahpRepository.mediaType(file.uri) } ?: continue
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:108:                    entries += ThumbnailEntry(file.uri, file.name, preview?.absolutePath, true, type, file.modifiedTime)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:130:    fun toggleThumbnailSelection(uri: Uri) {
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:132:            if (state.ahpBusy || state.ahpActive || state.thumbnails.none { it.uri == uri }) state
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:133:            else state.copy(thumbnailSelection = com.rec.gpiv.model.OrderedSelection.toggle(state.thumbnailSelection, uri))
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:138:        return com.rec.gpiv.model.OrderedSelection.entries(state.thumbnails, state.thumbnailSelection, ThumbnailEntry::uri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:197:                if (fileList.getTagFolderUri() == folder) {
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:248:        currentTreeUri = { fileList.getTagFolderUri() },
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:249:        currentFile = { fileList.current() },
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:273:        val folder = fileList.getTagFolderUri()
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:281:        // when the user navigates elsewhere during a long move.
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:286:            currentTreeUri = { folder },
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:287:            currentFile = { file },
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:303:                if (fileList.getTagFolderUri() == folder && tagsEnabled) {
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:311:                            fileTag = fileList.current()?.uri?.toString()?.let(fileTags::get)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:329:            fileList.getTagFolderUri()?.let {
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:376:        val treeUri =
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:377:            fileList.getTagFolderUri()
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:381:        tagFiles.ensureGpivTagsFile(treeUri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:382:        tagFiles.loadLisTags(treeUri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:385:        val currentFile =
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:391:                treeUri,
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:392:                currentFile.uri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:410:        val uri =
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:411:            pendingUri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:413:        if (uri != null) {
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:416:                "PlayerViewModel: carregando URI pendente = $uri"
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:419:            pendingUri = null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:421:            player.load(uri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:427:    fun onVideoSelected(uri: Uri) = handleIncomingUri(uri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:434:        if (selectedFile != null) load(Uri.fromFile(selectedFile)) else firstFile()
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:438:        val folder = fileList.getTagFolderUri() ?: return
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:440:        val currentUri = selectedUri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:447:        if (currentUri != null && fileList.setCurrent(currentUri)) {
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:453:                fileTag = if (tagsEnabled) tagFiles.readFileTag(folder, current.uri) else null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:462:        selectedUri = null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:463:        pendingUri = null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:473:    fun onFolderSelected(uri: Uri) {
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:475:        folderRepository.saveFolder(uri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:476:        loadFolder(uri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:480:    fun handleIncomingUri(uri: Uri) {
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:483:            val localFile = withContext(Dispatchers.IO) { folderRepository.findLocalFile(uri) }
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:490:                load(Uri.fromFile(localFile))
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:492:                folderRepository.findMatchingFolder(uri)?.let(::loadFolder)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:493:                load(uri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:503:                if (selectedUri == null) return@collect
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:570:                            selectedUri != null &&
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:594:    fun load(uri: Uri) {
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:607:                    if (uri.scheme == "file") file.uri == uri else try {
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:608:                        file.uri.authority == uri.authority && DocumentsContract.getDocumentId(file.uri) ==
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:609:                            DocumentsContract.getDocumentId(uri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:621:        val loadUri =
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:622:            fileFromList?.uri ?: uri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:626:                ?: if (uri.scheme == "file") java.io.File(requireNotNull(uri.path)).name else tagFiles.getFileName(uri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:629:            "PlayerViewModel: URI recebida = $uri"
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:633:            "PlayerViewModel: URI usada para carregar = $loadUri"
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:640:        selectedUri =
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:641:            loadUri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:651:            fileList.setCurrent(loadUri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:656:                fileTag = if (tagsEnabled) fileTags[loadUri.toString()] else null,
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:667:            player.load(loadUri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:671:            pendingUri = loadUri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:676:        uri: Uri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:680:            "PlayerViewModel: carregando pasta = $uri"
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:685:                uri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:689:            tagFiles.ensureGpivTagsFile(uri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:690:            tagFiles.loadLisTags(uri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:694:        val currentUri =
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:695:            selectedUri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:699:            currentUri != null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:704:                    currentUri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:709:                val currentFile =
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:712:                if (currentFile != null) {
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:717:                                uri,
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:718:                                currentFile.uri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:726:                            filename = currentFile.name,
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:742:        currentTreeUri = { fileList.getCurrentTreeUri() },
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:743:        currentFileName = { _uiState.value.filename },
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:744:        currentFileUri = { fileList.current()?.uri },
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:757:        currentTreeUri = { fileList.getTagFolderUri() },
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:759:        currentFileName = { _uiState.value.filename },
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:767:        currentTreeUri = { fileList.getCurrentTreeUri() }
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:770:    private fun getTrashUri(): Uri? = trashManager.findTrashUri()
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:772:    fun testTrashUri() {
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:773:        println("PlayerViewModel: testTrashUri = ${getTrashUri()}")
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:776:    private fun moveToTrash(uri: Uri): Boolean =
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:777:        trashManager.moveToTrash(uri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:780:        val uri = selectedUri ?: return false
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:782:        if (!permanent && !moveToTrash(uri)) {
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:788:            val nextFile = fileList.remove(uri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:789:            selectedUri = null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:790:            pendingUri = null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:810:        println("PlayerViewModel: apagando arquivo = $uri")
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:812:        val deleted = trashManager.deletePermanently(uri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:818:        val nextFile = fileList.remove(uri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:819:        selectedUri = null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:820:        pendingUri = null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:844:        if (file.uri.scheme == "file") {
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:845:            _uiState.update { it.copy(fileTag = if (tagsEnabled) fileTags[file.uri.toString()] else null) }
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:846:            load(file.uri)
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:850:        val treeUri =
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:851:            fileList.getCurrentTreeUri()
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:854:        selectedUri =
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:855:            file.uri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:860:                    treeUri,
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:861:                    file.uri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:879:            file.uri
    import java.util.Properties
    
    plugins {
        alias(libs.plugins.android.application)
        alias(libs.plugins.kotlin.compose)
    }
    
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use {
            localProperties.load(it)
        }
    }
    
    android {
        namespace = "com.rec.gpiv"
        ndkVersion = "28.2.13676358"
    
        compileSdk {
            version = release(36) {
                minorApiLevel = 1
            }
        }
    
        defaultConfig {
            applicationId = "com.rec.gpiv"
            minSdk = 24
            targetSdk = 36
            versionCode = 1
            versionName = "1.0"
    
            ndk {
                abiFilters += "arm64-v8a"
            }
    
            externalNativeBuild {
                cmake {
                    arguments(
                        "-DGPIV_MPV_ROOT=${localProperties.getProperty("GPIV_MPV_ROOT")}",
                        "-DGPIV_MPV_ANDROID_ROOT=${localProperties.getProperty("GPIV_MPV_ANDROID_ROOT")}",
                        "-DGPIV_FFMPEG_ROOT=${localProperties.getProperty("GPIV_FFMPEG_ROOT")}"
                    )
                }
            }
        }
    
        signingConfigs {
            getByName("debug") {
                // Mantém a mesma assinatura das instalações existentes, mesmo quando
                // o SDK usa XDG_CONFIG_HOME para escolher o keystore padrão.
                storeFile = file(localProperties.getProperty(
                    "GPIV_DEBUG_KEYSTORE",
                    "${System.getProperty("user.home")}/.android/debug.keystore"
                ))
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    
        buildTypes {
            release {
                optimization {
                    enable = false
                }
            }
        }
    
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    
        buildFeatures {
            compose = true
        }
    
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
            }
        }
    
    }
    
    dependencies {
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        testImplementation(libs.junit)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.compose.ui.test.junit4)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(libs.androidx.junit)
        debugImplementation(libs.androidx.compose.ui.test.manifest)
        debugImplementation(libs.androidx.compose.ui.tooling)
    }
    ✓ • 0ms

## Activity

    $ rg -n 'selectedUri|PlayerScreen\(|onRefreshFiles' app/src/main/java/com/rec/gpiv/MainActivity.kt app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt; git diff -- app/src/main/java/com/rec/gpiv/ui/player/PlayerControlsOverlay.kt
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:69:    private var selectedUri: Uri? = null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:440:        val currentUri = selectedUri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:462:        selectedUri = null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:503:                if (selectedUri == null) return@collect
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:570:                            selectedUri != null &&
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:640:        selectedUri =
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:695:            selectedUri
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:780:        val uri = selectedUri ?: return false
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:789:            selectedUri = null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:819:        selectedUri = null
    app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt:854:        selectedUri =
    app/src/main/java/com/rec/gpiv/MainActivity.kt:159:                    PlayerScreen(
    app/src/main/java/com/rec/gpiv/MainActivity.kt:168:                        onRefreshFiles = viewModel::refreshFiles,
    diff --git a/app/src/main/java/com/rec/gpiv/ui/player/PlayerControlsOverlay.kt b/app/src/main/java/com/rec/gpiv/ui/player/PlayerControlsOverlay.kt
    index 79ac886..b32988c 100644
    --- a/app/src/main/java/com/rec/gpiv/ui/player/PlayerControlsOverlay.kt
    +++ b/app/src/main/java/com/rec/gpiv/ui/player/PlayerControlsOverlay.kt
    @@ -197,19 +197,17 @@ fun PlayerControlsOverlay(
                         )
    
                         StatusText(
    -                        text = "&&&",
    +                        text = "${uiState.fileTag}",
                             modifier =
                                 Modifier.clickable {
    -                                onOpenFolder()
    +                                onRefreshFiles()
                                 }
                         )
    
                         StatusText(
    -                        text = "${uiState.fileTag}",
    +                        text = "@",
                             modifier =
    -                            Modifier.clickable {
    -                                onRefreshFiles()
    -                            }
    +                            Modifier.clickable { }
                         )
    
                     }
    @@ -497,14 +495,6 @@ fun PlayerControlsOverlay(
                                           }
                                   )
    
    -                              StatusText(
    -                                  text = "&&&",
    -                                  modifier =
    -                                      Modifier.clickable {
    -                                          onOpenFolder()
    -                                      }
    -                              )
    -
                             }
    
                             if (uiState.tagsEnabled) {
    ✓ • 0ms

## Assistant

O botão “@” agora abre o menu de compartilhamento do Android com o arquivo atual. Também configurei a permissão temporária de leitura para o aplicativo escolhido conseguir recebê-lo. Estou verificando a compilação e os testes.

## Activity

    $ ANDROID_HOME=/home/segodimo/Android/Sdk ./gradlew testDebugUnitTest assembleDebug --offline
    Starting a Gradle Daemon, 1 incompatible Daemon could not be reused, use --status for details
    Reusing configuration cache.
    > Task :app:generateDebugAssets UP-TO-DATE
    > Task :app:preBuild UP-TO-DATE
    > Task :app:preDebugUnitTestBuild UP-TO-DATE
    > Task :app:preDebugBuild UP-TO-DATE
    > Task :app:mergeDebugNativeDebugMetadata NO-SOURCE
    > Task :app:createDebugCompatibleScreenManifests UP-TO-DATE
    > Task :app:extractDeepLinksDebug UP-TO-DATE
    > Task :app:generateDebugGlobalSynthetics UP-TO-DATE
    > Task :app:javaPreCompileDebug UP-TO-DATE
    > Task :app:validateSigningDebug UP-TO-DATE
    > Task :app:writeDebugSigningConfigVersions UP-TO-DATE
    > Task :app:javaPreCompileDebugUnitTest UP-TO-DATE
    > Task :app:desugarDebugFileDependencies UP-TO-DATE
    > Task :app:writeDebugAppMetadata UP-TO-DATE
    > Task :app:mergeDebugJniLibFolders UP-TO-DATE
    > Task :app:generateDebugResources
    > Task :app:mapDebugSourceSetPaths
    > Task :app:checkDebugDuplicateClasses UP-TO-DATE
    > Task :app:mergeLibDexDebug UP-TO-DATE
    > Task :app:mergeDebugAssets UP-TO-DATE
    > Task :app:compressDebugAssets UP-TO-DATE
    > Task :app:checkDebugAarMetadata UP-TO-DATE
    > Task :app:processDebugNavigationResources
    > Task :app:compileDebugNavigationResources UP-TO-DATE
    > Task :app:mergeExtDexDebug UP-TO-DATE
    > Task :app:packageDebugResources
    > Task :app:parseDebugLocalResources
    > Task :app:processDebugMainManifest
    > Task :app:generateDebugRFile
    > Task :app:mergeDebugResources
    > Task :app:processDebugManifest
    > Task :app:processDebugManifestForPackage
    > Task :app:configureCMakeDebug[arm64-v8a]
    > Task :app:buildCMakeDebug[arm64-v8a]
    > Task :app:mergeDebugNativeLibs UP-TO-DATE
    > Task :app:stripDebugDebugSymbols UP-TO-DATE
    > Task :app:processDebugResources
    > Task :app:compileDebugKotlin
    > Task :app:compileDebugJavaWithJavac NO-SOURCE
    > Task :app:processDebugJavaRes UP-TO-DATE
    > Task :app:mergeDebugJavaResource UP-TO-DATE
    > Task :app:bundleDebugClassesToCompileJar
    > Task :app:bundleDebugClassesToRuntimeJar
    > Task :app:compileDebugUnitTestKotlin
    > Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
    > Task :app:processDebugUnitTestJavaRes UP-TO-DATE
    > Task :app:testDebugUnitTest
    > Task :app:dexBuilderDebug
    > Task :app:mergeProjectDexDebug
    > Task :app:packageDebug
    > Task :app:assembleDebug
    > Task :app:createDebugApkListingFileRedirect UP-TO-DATE
    
    BUILD SUCCESSFUL in 8s
    44 actionable tasks: 21 executed, 23 up-to-date
    Configuration cache entry reused.
    ✓ • 8.89s

## Activity

    $ git diff --check; git diff --stat
     app/src/main/AndroidManifest.xml                   |  9 ++++++
     app/src/main/java/com/rec/gpiv/MainActivity.kt     | 34 ++++++++++++++++++++++
     .../rec/gpiv/ui/player/PlayerControlsOverlay.kt    | 19 ++++--------
     .../java/com/rec/gpiv/ui/player/PlayerScreen.kt    |  2 ++
     .../java/com/rec/gpiv/viewmodel/PlayerViewModel.kt |  2 ++
     5 files changed, 52 insertions(+), 14 deletions(-)
    ✓ • 0ms

## Assistant

Pronto. Ao tocar em “@”, o app abre o menu de compartilhamento do Android com o arquivo atual.

Compilação do APK e testes unitários concluídos com sucesso.

## User

ao usar o navegador de arquivos quero um botão do lado de "abrir esta pasta" para incluir na lista as de arquivos, os arquivos que estiverem na pasta y nas subpastas no primeiro nivel

me confirme o que vc entendeu

## Assistant

Você quer um novo botão ao lado de **“Abrir esta pasta”** no navegador de arquivos.

Esse botão vai carregar na lista:
- Os arquivos da pasta atual.
- Os arquivos das subpastas diretamente dentro dela, somente no primeiro nível.

Não vai incluir arquivos de subpastas mais profundas. O botão **“Abrir esta pasta”** continua com o comportamento atual.

## User

vamos continuar assim

## Assistant

Vou adicionar esse botão ao navegador e ajustar o carregamento para incluir os arquivos da pasta atual e das subpastas de primeiro nível.

## Activity

    $ cat app/src/main/java/com/rec/gpiv/ui/browser/TerminalFileBrowser.kt
    package com.rec.gpiv.ui.browser
    
    import androidx.activity.ComponentDialog
    import androidx.activity.OnBackPressedCallback
    import android.content.Context
    import android.graphics.Color
    import android.graphics.Typeface
    import android.text.TextUtils
    import android.view.View
    import android.view.Gravity
    import android.view.ViewGroup
    import android.widget.Button
    import android.widget.LinearLayout
    import android.widget.BaseAdapter
    import android.widget.FrameLayout
    import android.widget.GridView
    import android.widget.TextView
    import com.rec.gpiv.R
    import java.io.File
    
    class TerminalFileBrowserDialog(context: Context) : ComponentDialog(context, R.style.Theme_Gpiv_FileBrowser) {
        var navigateBack: () -> Unit = {}
    
        init {
            onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = navigateBack()
            })
        }
    }
    
    /** Lista compacta em colunas, com diretórios marcados por uma barra final. */
    fun showTerminalFileBrowser(
        context: Context,
        directory: File,
        entries: List<File>,
        onOpen: (File) -> Unit,
        onSelectFolder: () -> Unit,
        onSelectDocumentTree: () -> Unit,
        existingDialog: TerminalFileBrowserDialog? = null
    ): TerminalFileBrowserDialog {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val parent = directory.parentFile?.takeIf { directory.absolutePath != "/storage" }
        val files = listOfNotNull(parent) + entries
        val labels = (if (parent != null) listOf("../") else emptyList()) +
            entries.map { it.name + if (it.isDirectory) "/" else "" }
        val dialog = existingDialog ?: TerminalFileBrowserDialog(context)
        dialog.navigateBack = { parent?.let(onOpen) }
        val title = TextView(dialog.context).apply {
            text = directory.absolutePath
            typeface = Typeface.MONOSPACE
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.MIDDLE
        }
        val nameNotice = TextView(dialog.context).apply {
            visibility = View.GONE
            setBackgroundColor(Color.BLACK)
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val hideNameNotice = Runnable { nameNotice.visibility = View.GONE }
        val grid = GridView(dialog.context).apply {
            setBackgroundColor(Color.BLACK)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            horizontalSpacing = dp(8)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            numColumns = 2
            adapter = object : BaseAdapter() {
                override fun getCount() = files.size
                override fun getItem(position: Int) = files[position]
                override fun getItemId(position: Int) = position.toLong()
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                    ((convertView as? TextView) ?: TextView(dialog.context).apply {
                        typeface = Typeface.MONOSPACE
                        textSize = 13f
                        setTextColor(Color.WHITE)
                        setPadding(dp(6), dp(6), dp(6), dp(6))
                        minHeight = dp(48)
                        setSingleLine()
                        ellipsize = TextUtils.TruncateAt.MIDDLE
                    }).apply {
                        text = labels[position]
                        contentDescription = if (files[position].isDirectory) "Pasta ${files[position].name}" else files[position].name
                    }
            }
            setOnItemClickListener { _, _, position, _ ->
                if (!files[position].isDirectory) dialog.dismiss()
                onOpen(files[position])
            }
            setOnItemLongClickListener { _, _, position, _ ->
                nameNotice.removeCallbacks(hideNameNotice)
                nameNotice.text = files[position].name
                nameNotice.visibility = View.VISIBLE
                nameNotice.postDelayed(hideNameNotice, 500L)
                true
            }
        }
        val container = FrameLayout(dialog.context).apply {
            setBackgroundColor(Color.BLACK)
            addView(grid, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }
        container.addView(nameNotice, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply {
            setMargins(dp(12), 0, dp(12), dp(12))
        })
        dialog.setOnDismissListener {
            nameNotice.removeCallbacks(hideNameNotice)
            nameNotice.visibility = View.GONE
        }
        val footer = LinearLayout(dialog.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(dp(6), dp(4), dp(6), dp(4))
        }
        fun action(label: String, dismiss: Boolean = true, onClick: () -> Unit) {
            footer.addView(Button(dialog.context).apply {
                text = label
                isAllCaps = false
                textSize = 12f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.TRANSPARENT)
                setPadding(dp(4), dp(8), dp(4), dp(8))
                minHeight = dp(48)
                setOnClickListener {
                    if (dismiss) dialog.dismiss()
                    onClick()
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        action("Tags e exportação", onClick = onSelectDocumentTree)
        parent?.let { pastaPai -> action("../", dismiss = false) { onOpen(pastaPai) } }
        action("Cancelar") {}
        action("Abrir esta pasta", onClick = onSelectFolder)
        val layout = LinearLayout(dialog.context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            fitsSystemWindows = true
            addView(title, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(container, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(footer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        dialog.setContentView(layout)
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawableResource(R.drawable.file_browser_background)
            decorView.clipToOutline = true
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        grid.post {
            // Mantém pelo menos duas colunas; telas maiores comportam mais nomes.
            grid.numColumns = ((grid.width - grid.paddingLeft - grid.paddingRight) / dp(160)).coerceIn(2, 5)
        }
        return dialog
    }
    ✓ • 0ms

## Activity

    $ cat app/src/main/java/com/rec/gpiv/player/FileList.kt; cat app/src/main/java/com/rec/gpiv/data/FolderRepository.kt; sed -n '100,165p' app/src/main/java/com/rec/gpiv/MainActivity.kt; sed -n '420,480p' app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt
    package com.rec.gpiv.player
    
    import android.content.ContentResolver
    import android.net.Uri
    import android.provider.DocumentsContract
    
    
    class FileList(
        private val contentResolver: ContentResolver
    ) {
    
        private val files =
            mutableListOf<FileItem>()
    
        private var currentIndex = -1
    
        private var currentTreeUri: Uri? = null
    
        /*
         * ========================================================
         * CARREGAR PASTA
         * ========================================================
         *
         * Recebe diretamente a treeUri obtida através de
         * ACTION_OPEN_DOCUMENT_TREE.
         *
         * Exemplo:
         *
         * content://com.android.externalstorage.documents/tree/
         * primary%3Aig_repit
         */
    
        fun loadFromTree(
            treeUri: Uri
        ): Boolean {
    
            files.clear()
            currentIndex = -1
            localDirectory = null
            currentTreeUri = treeUri
    
            println(
                "FileList: carregando árvore = $treeUri"
            )
    
    
            /*
             * ----------------------------------------------------
             * DOCUMENT ID DA PASTA
             * ----------------------------------------------------
             */
    
            val treeDocumentId =
                try {
    
                    DocumentsContract.getTreeDocumentId(
                        treeUri
                    )
    
                } catch (e: Exception) {
    
                    println(
                        "FileList: não foi possível obter " +
                            "treeDocumentId: ${e.message}"
                    )
    
                    return false
                }
    
    
            println(
                "FileList: treeDocumentId=$treeDocumentId"
            )
    
    
            /*
             * ----------------------------------------------------
             * URI DOS FILHOS
             * ----------------------------------------------------
             *
             * IMPORTANTE:
             *
             * Não usamos:
             *
             * buildChildDocumentsUri()
             *
             * Usamos:
             *
             * buildChildDocumentsUriUsingTree()
             *
             * porque temos uma URI de árvore autorizada.
             */
    
            val childrenUri =
                DocumentsContract
                    .buildChildDocumentsUriUsingTree(
                        treeUri,
                        treeDocumentId
                    )
    
    
            println(
                "FileList: childrenUri=$childrenUri"
            )
    
    
            /*
             * ----------------------------------------------------
             * CONSULTAR FILHOS
             * ----------------------------------------------------
             */
    
            try {
    
                contentResolver.query(
    
                    childrenUri,
    
                    arrayOf(
    
                        DocumentsContract.Document
                            .COLUMN_DOCUMENT_ID,
    
                        DocumentsContract.Document
                            .COLUMN_DISPLAY_NAME,
    
                        DocumentsContract.Document
                            .COLUMN_LAST_MODIFIED,
    
                        DocumentsContract.Document
                            .COLUMN_MIME_TYPE
                    ),
    
                    null,
                    null,
                    null
    
                )?.use { cursor ->
    
    
                    val idIndex =
                        cursor.getColumnIndex(
                            DocumentsContract.Document
                                .COLUMN_DOCUMENT_ID
                        )
    
    
                    val nameIndex =
                        cursor.getColumnIndex(
                            DocumentsContract.Document
                                .COLUMN_DISPLAY_NAME
                        )
    
    
                    val modifiedIndex =
                        cursor.getColumnIndex(
                            DocumentsContract.Document
                                .COLUMN_LAST_MODIFIED
                        )
    
    
                    val mimeIndex =
                        cursor.getColumnIndex(
                            DocumentsContract.Document
                                .COLUMN_MIME_TYPE
                        )
    
    
                    println(
                        "FileList: idIndex=$idIndex"
                    )
    
                    println(
                        "FileList: nameIndex=$nameIndex"
                    )
    
                    println(
                        "FileList: modifiedIndex=$modifiedIndex"
                    )
    
                    println(
                        "FileList: mimeIndex=$mimeIndex"
                    )
    
    
                    /*
                     * --------------------------------------------
                     * PERCORRER ARQUIVOS
                     * --------------------------------------------
                     */
    
                    while (cursor.moveToNext()) {
    
    
                        if (idIndex < 0) {
                            continue
                        }
    
    
                        val childId =
                            cursor.getString(
                                idIndex
                            )
    
    
                        val name =
                            if (nameIndex >= 0) {
    
                                cursor.getString(
                                    nameIndex
                                )
    
                            } else {
    
                                childId
                            }
    
    
                        val modifiedTime =
                            if (
                                modifiedIndex >= 0 &&
                                !cursor.isNull(
                                    modifiedIndex
                                )
                            ) {
    
                                cursor.getLong(
                                    modifiedIndex
                                )
    
                            } else {
    
                                0L
                            }
    
    
                        val mimeType =
                            if (
                                mimeIndex >= 0 &&
                                !cursor.isNull(
                                    mimeIndex
                                )
                            ) {
    
                                cursor.getString(
                                    mimeIndex
                                )
    
                            } else {
    
                                null
                            }
    
    
                        /*
                         * ----------------------------------------
                         * IGNORAR DIRETÓRIOS
                         * ----------------------------------------
                         *
                         * Neste primeiro teste queremos apenas
                         * os arquivos da pasta atual.
                         *
                         * Subpastas serão tratadas posteriormente.
                         */
    
                        if (
                            mimeType ==
                            DocumentsContract.Document
                                .MIME_TYPE_DIR
                        ) {
    
                            println(
                                "FileList: ignorando pasta = $name"
                            )
    
                            continue
                        }
    
                        if (name == "gpivtags.csv") {
    
                            println(
                                "FileList: ignorando arquivo de tags = $name"
                            )
    
                            continue
                        }
    
    
                        /*
                         * ----------------------------------------
                         * URI DO ARQUIVO
                         * ----------------------------------------
                         *
                         * IMPORTANTE:
                         *
                         * Usamos buildDocumentUriUsingTree()
                         * porque o acesso vem da treeUri.
                         */
    
                        val childUri =
                            DocumentsContract
                                .buildDocumentUriUsingTree(
                                    treeUri,
                                    childId
                                )
    
    
                        val file =
                            FileItem(
    
                                uri =
                                    childUri,
    
                                name =
                                    name,
    
                                modifiedTime =
                                    modifiedTime
                            )
    
    
                        files.add(
                            file
                        )
    
    
                        println(
                            "FileList: encontrado = " +
                                "$name " +
                                "modified=$modifiedTime " +
                                "mime=$mimeType " +
                                "uri=$childUri"
                        )
                    }
                }
    
    
            } catch (e: Exception) {
    
                println(
                    "FileList: erro ao listar arquivos: " +
                        e.message
                )
    
                return false
            }
    
    
            /*
             * ----------------------------------------------------
             * ORDENAR
             * ----------------------------------------------------
             *
             * Timestamp do nome decrescente; sem timestamp válido, modificação decrescente.
             */
    
            val ordered = FileNameOrder.sorted(files.toList(), FileItem::name, FileItem::modifiedTime)
            files.clear()
            files.addAll(ordered)
    
    
            /*
             * ----------------------------------------------------
             * RESULTADO
             * ----------------------------------------------------
             */
    
            println(
                "FileList: encontrados " +
                    "${files.size} arquivos"
            )
    
    
            for (index in files.indices) {
    
                val file =
                    files[index]
    
                println(
                    "FileList: [$index] " +
                        "${file.name} " +
                        "modified=${file.modifiedTime} " +
                        "uri=${file.uri}"
                )
            }
    
    
            /*
             * Não selecionamos nenhum arquivo ainda.
             *
             * O próximo passo será informar ao FileList qual
             * arquivo foi aberto pelo usuário para determinar
             * currentIndex.
             */
    
            currentIndex = -1
    
    
            return files.isNotEmpty()
        }
    
        fun clear() {
            files.clear()
            currentIndex = -1
            currentTreeUri = null
            localDirectory = null
        }
    
        fun loadFromDirectory(directory: java.io.File): Boolean {
            files.clear()
            currentIndex = -1
            currentTreeUri = null
            localDirectory = directory
            val children = directory.listFiles() ?: return false
            val items = children.filter { it.isFile && it.name != "gpivtags.csv" }
                .map { FileItem(Uri.fromFile(it), it.name, it.lastModified()) }
            files.addAll(FileNameOrder.sorted(items, FileItem::name, FileItem::modifiedTime))
            return files.isNotEmpty()
        }
    
        private var localDirectory: java.io.File? = null
    
        fun getTagFolderUri(): Uri? = currentTreeUri ?: localDirectory?.let(Uri::fromFile)
    
        fun getCurrentTreeUri(): Uri? {
            return currentTreeUri
        }
    
        fun setCurrent(
            uri: Uri
        ): Boolean {
    
            if (uri.scheme == "file") {
                currentIndex = files.indexOfFirst { it.uri == uri }
                return currentIndex >= 0
            }
    
            val targetDocumentId =
                try {
    
                    DocumentsContract.getDocumentId(
                        uri
                    )
    
                } catch (e: Exception) {
    
                    println(
                        "FileList: não foi possível obter " +
                            "documentId do arquivo selecionado: " +
                            e.message
                    )
    
                    return false
                }
    
            println(
                "FileList: procurando arquivo atual"
            )
    
            println(
                "FileList: targetDocumentId=$targetDocumentId"
            )
    
            for (index in files.indices) {
    
                val file =
                    files[index]
    
                val documentId =
                    try {
    
                        DocumentsContract.getDocumentId(
                            file.uri
                        )
    
                    } catch (e: Exception) {
    
                        println(
                            "FileList: erro obtendo documentId " +
                                "de ${file.name}: ${e.message}"
                        )
    
                        continue
                    }
    
                if (file.uri.authority == uri.authority && documentId == targetDocumentId) {
    
                    currentIndex = index
    
                    println(
                        "FileList: arquivo atual encontrado"
                    )
    
                    println(
                        "FileList: currentIndex=$currentIndex"
                    )
    
                    println(
                        "FileList: current=${file.name}"
                    )
    
                    return true
                }
            }
    
            println(
                "FileList: arquivo selecionado não foi " +
                    "encontrado na lista"
            )
    
            currentIndex = -1
    
            return false
        }
    
        /*
         * ========================================================
         * CURRENT
         * ========================================================
         */
    
        fun current(): FileItem? {
    
            if (currentIndex < 0) {
                return null
            }
    
            if (currentIndex >= files.size) {
                return null
            }
    
            return files[currentIndex]
        }
    
    
        /*
         * ========================================================
         * NEXT
         * ========================================================
         */
    
        fun next(): FileItem? {
    
            if (files.isEmpty()) {
                return null
            }
    
            if (currentIndex < 0) {
                return null
            }
    
            if (currentIndex >= files.lastIndex) {
                return null
            }
    
            currentIndex++
    
            return files[currentIndex]
        }
    
    
        /*
         * ========================================================
         * PREVIOUS
         * ========================================================
         */
    
        fun previous(): FileItem? {
    
            if (files.isEmpty()) {
                return null
            }
    
            if (currentIndex <= 0) {
                return null
            }
    
            currentIndex--
    
            return files[currentIndex]
        }
    
    
        /*
         * ========================================================
         * SIZE
         * ========================================================
         */
    
    
        /*
         * ========================================================
         * FIRST
         * ========================================================
         */
    
        fun first(): FileItem? {
    
            if (files.isEmpty()) {
                return null
            }
    
            currentIndex = 0
    
            return files[currentIndex]
        }
    
    
        /*
         * ========================================================
         * LAST
         * ========================================================
         */
    
        fun last(): FileItem? {
    
            if (files.isEmpty()) {
                return null
            }
    
            currentIndex = files.lastIndex
    
            return files[currentIndex]
        }
    
    
        /*
         * ========================================================
         * JUMP
         * ========================================================
         */
    
        fun jump(offset: Int): FileItem? {
    
            if (files.isEmpty()) {
                return null
            }
    
            if (currentIndex < 0) {
                return null
            }
    
            currentIndex =
                (currentIndex + offset).coerceIn(
                    0,
                    files.lastIndex
                )
    
            return files[currentIndex]
        }
    
        fun size(): Int {
    
            return files.size
        }
    
        /*
         * ========================================================
         * REMOVE
         * ========================================================
         */
        fun remove(
            uri: Uri
        ): FileItem? {
    
            val index =
                files.indexOfFirst {
                    it.uri == uri
                }
    
            if (index < 0) {
    
                println(
                    "FileList: arquivo não encontrado para remoção = $uri"
                )
    
                return current()
            }
    
            println(
                "FileList: removendo = ${files[index].name}"
            )
    
            files.removeAt(index)
    
            if (files.isEmpty()) {
    
                currentIndex = -1
    
                println(
                    "FileList: lista ficou vazia"
                )
    
                return null
            }
    
            /*
             * Se o arquivo removido estava antes
             * do arquivo atual, o índice atual diminui.
             */
            if (index < currentIndex) {
    
                currentIndex--
    
            } else if (index == currentIndex) {
    
                /*
                 * O próximo arquivo ocupa o mesmo índice.
                 *
                 * Se era o último arquivo, o novo último
                 * passa a ser o anterior.
                 */
                if (currentIndex >= files.size) {
                    currentIndex = files.lastIndex
                }
            }
    
            println(
                "FileList: novo currentIndex=$currentIndex"
            )
    
            return current()
        }
    
    
        /*
         * ========================================================
         * CURRENT INDEX
         * ========================================================
         */
    
        fun currentIndex(): Int {
    
            return currentIndex
        }
    
    
        /*
         * ========================================================
         * ALL
         * ========================================================
         */
    
        fun all(): List<FileItem> {
    
            return files.toList()
        }
    }
    package com.rec.gpiv.data
    
    import android.content.ContentResolver
    import android.content.Context
    import android.net.Uri
    import android.provider.DocumentsContract
    import android.provider.MediaStore
    import android.content.ContentUris
    import android.os.Environment
    import android.system.Os
    import java.io.File
    
    /**
     * Persiste as pastas escolhidas pelo usuário e resolve a pasta SAF que
     * corresponde a um arquivo recebido pelo sistema.
     */
    class FolderRepository(context: Context) {
    
        private val contentResolver: ContentResolver = context.contentResolver
    
        private val preferences = context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
    
        fun hasReadPermission(uri: Uri): Boolean =
            contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission
            }
    
        fun savedFolders(): List<Uri> =
            preferences.getStringSet(FOLDER_URIS_KEY, emptySet())
                .orEmpty()
                .map(Uri::parse)
    
        fun validSavedFolders(): List<Uri> =
            savedFolders().filter(::hasReadPermission)
    
        fun saveFolder(uri: Uri) {
            val folders = savedFolders()
                .toMutableSet()
                .apply { add(uri) }
    
            preferences.edit()
                .putStringSet(
                    FOLDER_URIS_KEY,
                    folders.map(Uri::toString).toSet()
                )
                .apply()
        }
    
        fun findMatchingFolder(uri: Uri): Uri? {
            val folders = validSavedFolders()
    
            findDocumentsProviderMatch(uri, folders)?.let { return it }
            return findExternalStorageMatch(uri, folders)
        }
    
        /** Resolve somente um arquivo real e acessível; nunca procura apenas pelo nome. */
        fun findLocalFile(uri: Uri): File? {
            fun accessible(path: String?): File? = runCatching {
                path?.let(::File)?.canonicalFile?.takeIf {
                    it.isFile && it.canRead() && it.parentFile?.canRead() == true
                }
            }.getOrNull()
    
            if (uri.scheme == "file") return accessible(uri.path)
            if (uri.scheme != "content") return null
    
            if (uri.authority == "com.android.externalstorage.documents") {
                val id = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
                if (id != null) {
                    val volume = id.substringBefore(':')
                    val root = if (volume == "primary") Environment.getExternalStorageDirectory()
                        else File("/storage/$volume")
                    accessible(File(root, id.substringAfter(':', "")).path)?.let { return it }
                }
            }
    
            fun queryPath(target: Uri): File? = runCatching {
                contentResolver.query(target, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use {
                    if (it.moveToFirst()) accessible(it.getString(0)) else null
                }
            }.getOrNull()
    
            queryPath(uri)?.let { return it }
            if (uri.authority == "com.android.providers.media.documents") {
                val id = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
                val number = id?.substringAfter(':')?.toLongOrNull()
                val collection = when (id?.substringBefore(':')) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> null
                }
                if (number != null && collection != null) {
                    queryPath(ContentUris.withAppendedId(collection, number))?.let { return it }
                }
            }
    
            // FileProvider de gerenciadores de arquivos pode não fornecer a coluna DATA.
            // O descritor aponta para o arquivo local quando o provedor não usa um pipe.
            runCatching {
                contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    accessible(Os.readlink("/proc/self/fd/${descriptor.fd}"))
                }
            }.getOrNull()?.let { return it }
    
            // Alguns gerenciadores enviam o caminho absoluto dentro da URI content.
            val path = uri.path
            if (path != null) {
                val storageIndex = path.indexOf("/storage/")
                if (storageIndex >= 0) accessible(path.substring(storageIndex))?.let { return it }
            }
            return null
        }
    
        private fun findDocumentsProviderMatch(
            fileUri: Uri,
            folders: List<Uri>
        ): Uri? {
            val documentId = try {
                DocumentsContract.getDocumentId(fileUri)
            } catch (_: Exception) {
                return null
            }
    
            return folders.firstOrNull { treeUri ->
                if (treeUri.authority != fileUri.authority) return@firstOrNull false
                val treeDocumentId = try {
                    DocumentsContract.getTreeDocumentId(treeUri)
                } catch (_: Exception) {
                    return@firstOrNull false
                }
    
                documentId == treeDocumentId ||
                    documentId.startsWith("$treeDocumentId/")
            }
        }
    
        private fun findExternalStorageMatch(
            fileUri: Uri,
            folders: List<Uri>
        ): Uri? {
            val path = fileUri.path ?: return null
            if (!path.startsWith(EXTERNAL_STORAGE_PREFIX)) return null
    
            val relativePath = path.removePrefix(EXTERNAL_STORAGE_PREFIX)
    
            return folders.firstOrNull { treeUri ->
                val folderPath = try {
                    DocumentsContract.getTreeDocumentId(treeUri)
                        .removePrefix(PRIMARY_PREFIX)
                        .trimEnd('/')
                } catch (_: Exception) {
                    return@firstOrNull false
                }
    
                relativePath == folderPath ||
                    relativePath.startsWith("$folderPath/")
            }
        }
    
        private companion object {
            const val PREFERENCES_NAME = "gpiv_preferences"
            const val FOLDER_URIS_KEY = "folder_uris"
            const val EXTERNAL_STORAGE_PREFIX = "/storage/emulated/0/"
            const val PRIMARY_PREFIX = "primary:"
        }
    }
        private fun hasStorageAccess(): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
            }
    
        private val storageSettingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (hasStorageAccess()) showStorageBrowser(browserDirectory)
            else showNotice("Ative o acesso a todos os arquivos para navegar pelas pastas.")
        }
    
        private val storagePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            if (hasStorageAccess()) showStorageBrowser(browserDirectory)
            else showNotice("O acesso ao armazenamento não foi autorizado.")
        }
    
        private fun openStorageBrowser() {
            if (hasStorageAccess()) {
                showStorageBrowser(browserDirectory)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                AlertDialog.Builder(this)
                    .setTitle("Acesso a todos os arquivos")
                    .setMessage("Autorize o GPIV na próxima tela para abrir arquivos e navegar entre pastas sem autorizar cada pasta.")
                    .setPositiveButton("Autorizar") { _, _ ->
                        try {
                            storageSettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:$packageName")))
                        } catch (_: ActivityNotFoundException) {
                            storageSettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } else {
                storagePermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE))
            }
        }
    
        private fun showStorageBrowser(directory: File) {
            lifecycleScope.launch {
                val entries = withContext(Dispatchers.IO) {
                    directory.listFiles()?.toList()
                        ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
                }
                if (isFinishing || isDestroyed) return@launch
                if (entries == null) {
                    showNotice("Esta pasta não está acessível.")
                    return@launch
                }
                browserDirectory = directory
                val roots = if (directory == Environment.getExternalStorageDirectory()) {
                    getExternalFilesDirs(null).mapNotNull { it?.absolutePath?.substringBefore("/Android/") }
                        .map(::File).filter { it != directory && it.isDirectory }
                } else emptyList()
                browserDialog = showTerminalFileBrowser(
                    context = this@MainActivity,
                    directory = directory,
                    entries = roots + entries,
                    onOpen = { item ->
    
                pendingUri = null
    
                player.load(uri)
            }
        }
    
        private var incomingFileJob: Job? = null
    
        fun onVideoSelected(uri: Uri) = handleIncomingUri(uri)
    
        fun onLocalFolderSelected(directory: java.io.File, selectedFile: java.io.File? = null) {
            incomingFileJob?.cancel()
            fileList.loadFromDirectory(directory)
            _uiState.update { it.copy(fileTag = null, tags = emptyList(), fileIndex = -1, fileCount = fileList.size()) }
            reloadTags()
            if (selectedFile != null) load(Uri.fromFile(selectedFile)) else firstFile()
        }
    
        fun refreshFiles() {
            val folder = fileList.getTagFolderUri() ?: return
            incomingFileJob?.cancel()
            val currentUri = selectedUri
            if (folder.scheme == "file") {
                fileList.loadFromDirectory(java.io.File(requireNotNull(folder.path)))
            } else {
                fileList.loadFromTree(folder)
            }
            reloadTags()
            if (currentUri != null && fileList.setCurrent(currentUri)) {
                val current = requireNotNull(fileList.current())
                _uiState.update { it.copy(
                    filename = current.name,
                    fileIndex = fileList.currentIndex(),
                    fileCount = fileList.size(),
                    fileTag = if (tagsEnabled) tagFiles.readFileTag(folder, current.uri) else null
                ) }
            } else {
                firstFile()
            }
        }
    
        private fun clearCurrentFile() {
            player.pause()
            selectedUri = null
            pendingUri = null
            markerController.clear()
            _uiState.update { it.copy(
                filename = null, fileTag = null, fileIndex = -1, fileCount = 0,
                playing = false, loading = false, position = 0.0, duration = 0.0,
                currentFrame = 0L, previousKeyframeFrame = null,
                markerASet = false, markerBSet = false
            ) }
        }
    
        fun onFolderSelected(uri: Uri) {
            incomingFileJob?.cancel()
            folderRepository.saveFolder(uri)
            loadFolder(uri)
            firstFile()
        }
    ✓ • 0ms

## Activity

    $ sed -n '160,181p' app/src/main/java/com/rec/gpiv/MainActivity.kt; sed -n '590,625p' app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt; rg -n 'loadFromDirectory|fun firstFile|clearCurrentFile|reloadTags' app/src/main/java/com/rec/gpiv/viewmodel/PlayerViewModel.kt; cat app/src/test/java/com/rec/gpiv/FileNameOrderTest.kt
                } else emptyList()
                browserDialog = showTerminalFileBrowser(
                    context = this@MainActivity,
                    directory = directory,
                    entries = roots + entries,
                    onOpen = { item ->
                        if (item.isDirectory) showStorageBrowser(item)
                        else viewModel.onLocalFolderSelected(directory, item)
                    },
                    onSelectFolder = { viewModel.onLocalFolderSelected(directory) },
                    onSelectDocumentTree = { openFolderLauncher.launch(null) },
                    existingDialog = browserDialog?.takeIf { it.isShowing }
                )
            }
        }
    
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            )
                        }
                    }
                }
            }
        }
    
        fun load(uri: Uri) {
    
            /*
             * Primeiro tentamos encontrar o arquivo na FileList
             * através do Document ID.
             *
             * Isso funciona quando a URI recebida também é uma
             * URI SAF/DocumentsContract.
             */
            var fileFromList =
                fileList
                    .all()
                    .firstOrNull { file ->
                        if (uri.scheme == "file") file.uri == uri else try {
                            file.uri.authority == uri.authority && DocumentsContract.getDocumentId(file.uri) ==
                                DocumentsContract.getDocumentId(uri)
                        } catch (e: Exception) {
                            false
                        }
                    }
    
            /*
             * Se encontramos o arquivo na FileList, usamos a URI SAF
             * oficial criada a partir da pasta autorizada.
             *
             * Caso contrário, mantemos a URI original.
             */
            val loadUri =
                fileFromList?.uri ?: uri
    
    101:                if (folder.scheme == "file") fileList.loadFromDirectory(java.io.File(requireNotNull(folder.path)))
    200:                    if (folder.scheme == "file") fileList.loadFromDirectory(java.io.File(requireNotNull(folder.path)))
    202:                    reloadTags()
    327:    private fun reloadTags() {
    433:        fileList.loadFromDirectory(directory)
    435:        reloadTags()
    444:            fileList.loadFromDirectory(java.io.File(requireNotNull(folder.path)))
    448:        reloadTags()
    462:    private fun clearCurrentFile() {
    490:                fileList.loadFromDirectory(requireNotNull(localFile.parentFile))
    491:                reloadTags()
    919:    fun firstFile() {
    928:            clearCurrentFile()
    package com.rec.gpiv
    
    import com.rec.gpiv.player.FileNameOrder
    import java.text.SimpleDateFormat
    import java.util.Locale
    import org.junit.Assert.assertEquals
    import org.junit.Test
    
    class FileNameOrderTest {
        private data class Entry(val name: String, val modified: Long)
        private fun time(value: String) = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).parse(value)!!.time
        private fun sorted(vararg items: Entry) = FileNameOrder.sorted(items.toList(), Entry::name, Entry::modified).map { it.name }
    
        @Test fun timestampOverridesModificationDateAndIgnoresFolderAndType() {
            val older = Entry(".ZIm20261013120000_1010", time("20261015120000"))
            val newer = Entry(".AMv20261014120000_1010", 0)
            assertEquals(listOf(newer.name, older.name), sorted(older, newer))
        }
    
        @Test fun fallbackDatesMixWithNamedTimestampsInDescendingOrder() {
            val older = Entry("old.jpg", time("20261012120000"))
            val named = Entry(".FolderIm20261013120000_1010", 0)
            val newer = Entry("new.mp4", time("20261014120000"))
            assertEquals(listOf(newer.name, named.name, older.name), sorted(older, named, newer))
        }
    
        @Test fun invalidDatesAndDigitRunsUseModificationTime() {
            val invalid = Entry(".FolderIm20260230120000_1010", 300)
            val digits = Entry("12320261013120000999.jpg", 200)
            val plain = Entry("plain.jpg", 100)
            assertEquals(listOf(invalid.name, digits.name, plain.name), sorted(plain, digits, invalid))
        }
    
        @Test fun sameTimestampKeepsNumericSequenceAscendingAcrossMediaTypes() {
            val late = Entry(".FolderIm20261013120000_10002", 400)
            val early = Entry(".FolderMv20261013120000_9998", 0)
            assertEquals(listOf(early.name, late.name), sorted(late, early))
        }
    
        @Test fun fallbackTiesAreDeterministicByName() {
            assertEquals(listOf("a.jpg", "b.jpg"), sorted(Entry("b.jpg", 0), Entry("a.jpg", 0)))
        }
    }
    ✓ • 0ms

## Activity

    $ python3 - <<'PY'
        from pathlib import Path
        b=Path('app/src/main/java/com/rec/gpiv')
        p=b/'ui/browser/TerminalFileBrowser.kt';s=p.read_text().replace('    onSelectFolder: () -> Unit,','    onSelectFolder: () -> Unit,\n    onSelectFolderWithSubfolders: () -> Unit,').replace('    action("Abrir esta pasta", onClick = onSelectFolder)','    action("Abrir esta pasta", onClick = onSelectFolder)\n    action("Pasta + subpastas (1 nível)", onClick = onSelectFolderWithSubfolders)');p.write_text(s)
        p=b/'MainActivity.kt';s=p.read_text().replace('                onSelectFolder = { viewModel.onLocalFolderSelected(directory) },','                onSelectFolder = { viewModel.onLocalFolderSelected(directory) },\n                onSelectFolderWithSubfolders = {\n                    viewModel.onLocalFolderSelected(directory, includeSubfolders = true)\n                },');p.write_text(s)
        p=b/'viewmodel/PlayerViewModel.kt';s=p.read_text().replace('    fun onLocalFolderSelected(directory: java.io.File, selectedFile: java.io.File? = null) {','    fun onLocalFolderSelected(\n        directory: java.io.File,\n        selectedFile: java.io.File? = null,\n        includeSubfolders: Boolean = false\n    ) {').replace('        fileList.loadFromDirectory(directory)\n','        fileList.loadFromDirectory(directory, includeSubfolders)\n');p.write_text(s)
        p=b/'player/FileList.kt';s=p.read_text().replace('    fun loadFromDirectory(directory: java.io.File): Boolean {','''    fun loadFromDirectory(
                directory: java.io.File,
                includeSubfolders: Boolean = directory == localDirectory && localIncludesSubfolders
            ): Boolean {''').replace('        localDirectory = directory\n        val children = directory.listFiles() ?: return false\n        val items = children.filter { it.isFile && it.name != "gpivtags.csv" }','        localDirectory = directory\n        localIncludesSubfolders = includeSubfolders\n        val items = LocalFolderFiles.list(directory, includeSubfolders)').replace('    private var localDirectory: java.io.File? = null','    private var localDirectory: java.io.File? = null\n    private var localIncludesSubfolders = false');p.write_text(s)
        (b/'player/LocalFolderFiles.kt').write_text('''package com.rec.gpiv.player
        
        import java.io.File
        
        /** Arquivos da pasta e, opcionalmente, de suas subpastas imediatas. */
        object LocalFolderFiles {
            fun list(directory: File, includeSubfolders: Boolean): List<File> {
                val children = directory.listFiles().orEmpty()
                val candidates = children.asSequence().flatMap { child ->
                    if (includeSubfolders && child.isDirectory) {
                        child.listFiles().orEmpty().asSequence()
                    } else {
                        sequenceOf(child)
                    }
                }
                return candidates.filter { it.isFile && it.name != "gpivtags.csv" }.toList()
            }
        }
        ''')
        PY
    ✓ • 0ms

## Activity

    $ cat > app/src/test/java/com/rec/gpiv/LocalFolderFilesTest.kt <<'EOF'
        package com.rec.gpiv
        
        import com.rec.gpiv.player.LocalFolderFiles
        import org.junit.Assert.assertEquals
        import org.junit.Rule
        import org.junit.Test
        import org.junit.rules.TemporaryFolder
        
        class LocalFolderFilesTest {
            @get:Rule val temporaryFolder = TemporaryFolder()
        
            @Test fun includesOnlyImmediateSubfoldersAndKeepsDuplicateNames() {
                val root = temporaryFolder.root
                temporaryFolder.newFile("same.jpg")
                temporaryFolder.newFile("gpivtags.csv")
                temporaryFolder.newFolder("child")
                temporaryFolder.newFile("child/same.jpg")
                temporaryFolder.newFile("child/gpivtags.csv")
                temporaryFolder.newFolder("child", "deeper")
                temporaryFolder.newFile("child/deeper/excluded.jpg")
                temporaryFolder.newFolder("empty")
                temporaryFolder.newFolder("sibling")
                temporaryFolder.newFile("sibling/video.mp4")
        
                val paths = LocalFolderFiles.list(root, true).map { it.relativeTo(root).path }.toSet()
                assertEquals(setOf("same.jpg", "child/same.jpg", "sibling/video.mp4"), paths)
                assertEquals(listOf("same.jpg"), LocalFolderFiles.list(root, false).map { it.name })
            }
        }
        EOF
        ANDROID_HOME=/home/segodimo/Android/Sdk ./gradlew testDebugUnitTest assembleDebug --offline
    Reusing configuration cache.
    > Task :app:preBuild UP-TO-DATE
    > Task :app:generateDebugAssets UP-TO-DATE
    > Task :app:preDebugBuild UP-TO-DATE
    > Task :app:preDebugUnitTestBuild UP-TO-DATE
    > Task :app:extractDeepLinksDebug UP-TO-DATE
    > Task :app:javaPreCompileDebugUnitTest UP-TO-DATE
    > Task :app:validateSigningDebug UP-TO-DATE
    > Task :app:generateDebugGlobalSynthetics UP-TO-DATE
    > Task :app:javaPreCompileDebug UP-TO-DATE
    > Task :app:desugarDebugFileDependencies UP-TO-DATE
    > Task :app:writeDebugAppMetadata UP-TO-DATE
    > Task :app:writeDebugSigningConfigVersions UP-TO-DATE
    > Task :app:createDebugCompatibleScreenManifests UP-TO-DATE
    > Task :app:mergeDebugJniLibFolders UP-TO-DATE
    > Task :app:mapDebugSourceSetPaths UP-TO-DATE
    > Task :app:generateDebugResources UP-TO-DATE
    > Task :app:mergeDebugNativeDebugMetadata NO-SOURCE
    > Task :app:packageDebugResources UP-TO-DATE
    > Task :app:checkDebugDuplicateClasses UP-TO-DATE
    > Task :app:mergeLibDexDebug UP-TO-DATE
    > Task :app:mergeDebugAssets UP-TO-DATE
    > Task :app:processDebugNavigationResources UP-TO-DATE
    > Task :app:compressDebugAssets UP-TO-DATE
    > Task :app:checkDebugAarMetadata UP-TO-DATE
    > Task :app:mergeDebugResources UP-TO-DATE
    > Task :app:compileDebugNavigationResources UP-TO-DATE
    > Task :app:parseDebugLocalResources UP-TO-DATE
    > Task :app:generateDebugRFile UP-TO-DATE
    > Task :app:processDebugMainManifest UP-TO-DATE
    > Task :app:processDebugManifest UP-TO-DATE
    > Task :app:mergeExtDexDebug UP-TO-DATE
    > Task :app:processDebugManifestForPackage UP-TO-DATE
    > Task :app:processDebugResources UP-TO-DATE
    > Task :app:configureCMakeDebug[arm64-v8a]
    > Task :app:buildCMakeDebug[arm64-v8a]
    > Task :app:mergeDebugNativeLibs UP-TO-DATE
    > Task :app:stripDebugDebugSymbols UP-TO-DATE
    > Task :app:compileDebugKotlin
    > Task :app:compileDebugJavaWithJavac NO-SOURCE
    > Task :app:processDebugJavaRes UP-TO-DATE
    > Task :app:bundleDebugClassesToCompileJar
    > Task :app:mergeDebugJavaResource UP-TO-DATE
    > Task :app:bundleDebugClassesToRuntimeJar
    > Task :app:compileDebugUnitTestKotlin
    > Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
    > Task :app:processDebugUnitTestJavaRes UP-TO-DATE
    > Task :app:testDebugUnitTest
    > Task :app:dexBuilderDebug
    > Task :app:mergeProjectDexDebug
    > Task :app:packageDebug
    > Task :app:assembleDebug
    > Task :app:createDebugApkListingFileRedirect UP-TO-DATE
    
    BUILD SUCCESSFUL in 3s
    44 actionable tasks: 10 executed, 34 up-to-date
    Configuration cache entry reused.
    ✓ • 2.98s

## Assistant

O novo botão carrega os arquivos da pasta atual e das subpastas imediatas, mantendo essa opção ao atualizar a lista. Estou finalizando a disposição dos botões e verificando que pastas de níveis mais profundos ficam de fora.

## Activity

    $ python3 - <<'PY'
        from pathlib import Path
        p=Path('app/src/main/java/com/rec/gpiv/ui/browser/TerminalFileBrowser.kt')
        s=p.read_text().replace('    fun action(label: String, dismiss: Boolean = true, onClick: () -> Unit) {\n        footer.addView','''    val folderActions = LinearLayout(dialog.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.BLACK)
                setPadding(dp(6), dp(4), dp(6), dp(4))
            }
            fun action(
                label: String,
                dismiss: Boolean = true,
                row: LinearLayout = footer,
                onClick: () -> Unit
            ) {
                row.addView''').replace('action("Abrir esta pasta", onClick = onSelectFolder)','action("Abrir esta pasta", row = folderActions, onClick = onSelectFolder)').replace('action("Pasta + subpastas (1 nível)", onClick = onSelectFolderWithSubfolders)','action("Pasta + subpastas (1 nível)", row = folderActions, onClick = onSelectFolderWithSubfolders)').replace('        addView(footer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))','        addView(footer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))\n        addView(folderActions, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))');p.write_text(s)
        PY
        git diff --check
    ✓ • 0ms

## Activity

    $ ANDROID_HOME=/home/segodimo/Android/Sdk ./gradlew assembleDebug --offline
    Calculating task graph as no cached configuration is available for tasks: assembleDebug
    > Task :app:generateDebugAssets UP-TO-DATE
    > Task :app:preBuild UP-TO-DATE
    > Task :app:preDebugBuild UP-TO-DATE
    > Task :app:mergeDebugNativeDebugMetadata NO-SOURCE
    > Task :app:generateDebugGlobalSynthetics UP-TO-DATE
    > Task :app:validateSigningDebug UP-TO-DATE
    > Task :app:javaPreCompileDebug UP-TO-DATE
    > Task :app:createDebugCompatibleScreenManifests UP-TO-DATE
    > Task :app:mergeDebugJniLibFolders UP-TO-DATE
    > Task :app:writeDebugAppMetadata UP-TO-DATE
    > Task :app:mapDebugSourceSetPaths UP-TO-DATE
    > Task :app:extractDeepLinksDebug UP-TO-DATE
    > Task :app:desugarDebugFileDependencies UP-TO-DATE
    > Task :app:generateDebugResources UP-TO-DATE
    > Task :app:writeDebugSigningConfigVersions UP-TO-DATE
    > Task :app:checkDebugAarMetadata UP-TO-DATE
    > Task :app:mergeDebugAssets UP-TO-DATE
    > Task :app:checkDebugDuplicateClasses UP-TO-DATE
    > Task :app:processDebugNavigationResources UP-TO-DATE
    > Task :app:mergeLibDexDebug UP-TO-DATE
    > Task :app:compressDebugAssets UP-TO-DATE
    > Task :app:configureCMakeDebug[arm64-v8a]
    > Task :app:packageDebugResources UP-TO-DATE
    > Task :app:compileDebugNavigationResources UP-TO-DATE
    > Task :app:parseDebugLocalResources UP-TO-DATE
    > Task :app:mergeDebugResources UP-TO-DATE
    > Task :app:processDebugMainManifest UP-TO-DATE
    > Task :app:processDebugManifest UP-TO-DATE
    > Task :app:generateDebugRFile UP-TO-DATE
    > Task :app:processDebugManifestForPackage UP-TO-DATE
    > Task :app:mergeExtDexDebug UP-TO-DATE
    > Task :app:buildCMakeDebug[arm64-v8a]
    > Task :app:processDebugResources UP-TO-DATE
    > Task :app:mergeDebugNativeLibs UP-TO-DATE
    > Task :app:stripDebugDebugSymbols UP-TO-DATE
    > Task :app:compileDebugKotlin
    > Task :app:compileDebugJavaWithJavac NO-SOURCE
    > Task :app:processDebugJavaRes UP-TO-DATE
    > Task :app:mergeDebugJavaResource UP-TO-DATE
    > Task :app:dexBuilderDebug
    > Task :app:mergeProjectDexDebug
    > Task :app:packageDebug
    > Task :app:assembleDebug
    > Task :app:createDebugApkListingFileRedirect UP-TO-DATE
    
    BUILD SUCCESSFUL in 2s
    38 actionable tasks: 6 executed, 32 up-to-date
    Configuration cache entry stored.
    ✓ • 2.61s

## Assistant

Adicionei **“Pasta + subpastas (1 nível)”** ao lado de **“Abrir esta pasta”**.

Ele carrega os arquivos da pasta atual e das subpastas imediatas, sem entrar em níveis mais profundos. A opção é mantida ao atualizar a lista.

Testes passaram e o APK foi compilado com sucesso.

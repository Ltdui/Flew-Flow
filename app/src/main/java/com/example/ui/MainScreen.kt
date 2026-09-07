package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MicState
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.ExportShareHelper
import com.example.ui.components.HistoryBottomSheet
import com.example.ui.components.MicrophoneFab
import com.example.ui.components.RecordingStatusBar
import com.example.ui.components.SettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedTranscripts by viewModel.savedTranscripts.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        } else {
            // Permission denied
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissError()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Whisper Flow",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.createNewDocument() },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                .testTag("new_document_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                                contentDescription = "New document",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { showHistorySheet = true },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                .testTag("history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = "Saved Transcripts",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 900.dp)
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Language quick filter pills from Elegant Dark design HTML
                val languageOptions = listOf(
                    "auto" to "Auto Detect",
                    "en" to "English",
                    "bn" to "বাংলা",
                    "hi" to "हिन्दी",
                    "es" to "Español"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languageOptions.forEach { (code, label) ->
                        val isSelected = uiState.settings.languageCode == code
                        Surface(
                            onClick = { viewModel.updateSettings(uiState.settings.copy(languageCode = code)) },
                            shape = RoundedCornerShape(100.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Transcript Document Canvas with 32dp corners and border
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("transcript_editor_card"),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    val scrollState = rememberScrollState()

                    LaunchedEffect(uiState.interimText, uiState.finalizedText) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (uiState.finalizedText.isEmpty() && uiState.interimText.isEmpty()) {
                                // Empty state matching design aesthetic
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Mic,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                        Text(
                                            text = "Tap the microphone and speak naturally",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Near real-time transcription with Gemini SMART grammar, verbal pause removal, and spoken list formatting.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.widthIn(max = 380.dp)
                                        )
                                    }
                                }
                            } else {
                                // Finalized Clean Text
                                BasicTextField(
                                    value = uiState.finalizedText,
                                    onValueChange = { viewModel.onTextEdited(it) },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 18.sp,
                                        lineHeight = 28.sp
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("transcript_text_input")
                                )
                            }
                        }

                        // Live Interim Stream Section from Design HTML
                        if (uiState.interimText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "\"${uiState.interimText}\"",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 17.sp,
                                        lineHeight = 24.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "INTERIM STREAM",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 2.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Toolbar for Editor Actions & Metrics
                EditorToolbar(
                    canUndo = viewModel.canUndo,
                    canRedo = viewModel.canRedo,
                    wordCount = uiState.wordCount,
                    characterCount = uiState.characterCount,
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onCopy = {
                        val text = uiState.finalizedText.ifBlank { uiState.interimText }
                        ExportShareHelper.copyToClipboard(context, text)
                    },
                    onShare = {
                        val text = uiState.finalizedText.ifBlank { uiState.interimText }
                        ExportShareHelper.shareText(context, text, uiState.documentTitle)
                    },
                    onSmartPolish = { viewModel.runSmartPolish() },
                    isSmartPolishing = uiState.isSmartPolishing,
                    onClear = { viewModel.clearTranscript() },
                    showExportMenu = showExportMenu,
                    onToggleExportMenu = { showExportMenu = !showExportMenu },
                    onExportTxt = {
                        ExportShareHelper.exportAsTxt(context, uiState.finalizedText, uiState.documentTitle)
                        showExportMenu = false
                    },
                    onExportMarkdown = {
                        ExportShareHelper.exportAsMarkdown(context, uiState.finalizedText, uiState.documentTitle)
                        showExportMenu = false
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Waveform Audio Activity Visualizer
                AudioWaveformVisualizer(
                    audioLevel = uiState.audioLevel,
                    isRecording = uiState.micState == MicState.RECORDING,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Voice UI Status Bar
                RecordingStatusBar(
                    micState = uiState.micState,
                    connectionState = uiState.connectionState,
                    durationSeconds = uiState.recordingDurationSeconds,
                    isSmartPolishing = uiState.isSmartPolishing,
                    isSmartTranscription = uiState.settings.isSmartTranscription,
                    selectedLanguage = uiState.settings.languageCode
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Center with Flanking Action Buttons and Center Microphone FAB
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flanking Copy Button
                        IconButton(
                            onClick = {
                                val text = uiState.finalizedText.ifBlank { uiState.interimText }
                                ExportShareHelper.copyToClipboard(context, text)
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                .testTag("bottom_copy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy text",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(28.dp))

                        // Center Prominent Microphone Action Center
                        MicrophoneFab(
                            isRecording = uiState.micState == MicState.RECORDING,
                            audioLevel = uiState.audioLevel,
                            onClick = {
                                val permissionCheck = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                )
                                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.toggleRecording()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(28.dp))

                        // Flanking Share Button
                        IconButton(
                            onClick = {
                                val text = uiState.finalizedText.ifBlank { uiState.interimText }
                                ExportShareHelper.shareText(context, text, uiState.documentTitle)
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                .testTag("bottom_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share transcript",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Elegant Timer & Listening Caption
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            val minutes = uiState.recordingDurationSeconds / 60
                            val seconds = uiState.recordingDurationSeconds % 60
                            val formattedDuration = String.format("%02d:%02d", minutes, seconds)
                            Text(
                                text = if (uiState.micState == MicState.RECORDING) formattedDuration else "00:00",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = when (uiState.micState) {
                                MicState.RECORDING -> "LISTENING & REFINING"
                                else -> if (uiState.settings.isSmartTranscription) "GEMINI SMART STREAM" else "READY TO RECORD"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Home indicator bar from Design HTML
                    Box(
                        modifier = Modifier
                            .width(128.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = uiState.settings,
            hasBuildConfigApiKey = viewModel.hasBuildConfigApiKey,
            onSaveSettings = { viewModel.updateSettings(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showHistorySheet) {
        HistoryBottomSheet(
            transcripts = savedTranscripts,
            onSelectTranscript = { viewModel.loadTranscript(it) },
            onDeleteTranscript = { viewModel.deleteTranscript(it) },
            onDismiss = { showHistorySheet = false }
        )
    }
}

@Composable
private fun EditorToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    wordCount: Int,
    characterCount: Int,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSmartPolish: () -> Unit,
    isSmartPolishing: Boolean,
    onClear: () -> Unit,
    showExportMenu: Boolean,
    onToggleExportMenu: () -> Unit,
    onExportTxt: () -> Unit,
    onExportMarkdown: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo / Redo / Clear
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                }
                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                }
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Clear text",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Word / Character Counter
            Text(
                text = "$wordCount words • $characterCount chars",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            // Right actions: Smart Polish, Copy, Share, Export
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onSmartPolish,
                    enabled = !isSmartPolishing,
                    modifier = Modifier.testTag("smart_polish_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Smart Grammar Polish",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy all",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box {
                    IconButton(onClick = onToggleExportMenu) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Export options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = onToggleExportMenu
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export as TXT") },
                            leadingIcon = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                            onClick = onExportTxt
                        )
                        DropdownMenuItem(
                            text = { Text("Export as Markdown") },
                            leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) },
                            onClick = onExportMarkdown
                        )
                    }
                }
            }
        }
    }
}

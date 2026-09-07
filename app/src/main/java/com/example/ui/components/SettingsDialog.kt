package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppThemeMode
import com.example.data.model.SupportedLanguage
import com.example.data.model.TranscriptSettings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    currentSettings: TranscriptSettings,
    hasBuildConfigApiKey: Boolean,
    onSaveSettings: (TranscriptSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var settings by remember { mutableStateOf(currentSettings) }
    var languageExpanded by remember { mutableStateOf(false) }
    var newVocabTerm by remember { mutableStateOf("") }
    var showApiKeyInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Transcription Settings", fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Language Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Language", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        ExposedDropdownMenuBox(
                            expanded = languageExpanded,
                            onExpandedChange = { languageExpanded = !languageExpanded }
                        ) {
                            val selectedLanguage = SupportedLanguage.SUPPORTED_LANGUAGES.find {
                                it.code == settings.languageCode
                            } ?: SupportedLanguage.SUPPORTED_LANGUAGES.first()

                            OutlinedTextField(
                                value = "${selectedLanguage.displayName} (${selectedLanguage.nativeName})",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("language_selector"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = languageExpanded,
                                onDismissRequest = { languageExpanded = false }
                            ) {
                                SupportedLanguage.SUPPORTED_LANGUAGES.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text("${lang.displayName} - ${lang.nativeName}") },
                                        onClick = {
                                            settings = settings.copy(
                                                languageCode = lang.code,
                                                isAutoLanguageDetection = (lang.code == "auto")
                                            )
                                            languageExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto-detect language", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = settings.isAutoLanguageDetection,
                                onCheckedChange = { checked ->
                                    settings = settings.copy(
                                        isAutoLanguageDetection = checked,
                                        languageCode = if (checked) "auto" else settings.languageCode
                                    )
                                }
                            )
                        }
                    }
                }

                // 2. SMART Transcription Controls
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Spellcheck, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Gemini SMART Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Smart Transcription", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Grammar, self-correction, list auto-formatting", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Switch(
                                checked = settings.isSmartTranscription,
                                onCheckedChange = { settings = settings.copy(isSmartTranscription = it) }
                            )
                        }

                        if (settings.isSmartTranscription) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Auto punctuation & casing", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = settings.isAutoPunctuation,
                                    onCheckedChange = { settings = settings.copy(isAutoPunctuation = it) }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Remove filler words ('um', 'uh')", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = settings.isRemoveFillerWords,
                                    onCheckedChange = { settings = settings.copy(isRemoveFillerWords = it) }
                                )
                            }
                        }
                    }
                }

                // 3. Custom Vocabulary
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Custom Vocabulary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Add domain terms, technical acronyms, or specific names:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newVocabTerm,
                                onValueChange = { newVocabTerm = it },
                                placeholder = { Text("e.g. Kubernetes, PyTorch, Supabase") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val trimmed = newVocabTerm.trim()
                                    if (trimmed.isNotEmpty() && !settings.customVocabulary.contains(trimmed)) {
                                        settings = settings.copy(customVocabulary = settings.customVocabulary + trimmed)
                                        newVocabTerm = ""
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add term")
                            }
                        }

                        if (settings.customVocabulary.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                settings.customVocabulary.forEach { term ->
                                    InputChip(
                                        selected = true,
                                        onClick = {
                                            settings = settings.copy(customVocabulary = settings.customVocabulary - term)
                                        },
                                        label = { Text(term) },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Remove $term",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Appearance (Theme)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = settings.themeMode == mode,
                                    onClick = { settings = settings.copy(themeMode = mode) },
                                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                    }
                }

                // 5. API Key status
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Gemini API Key", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        val keyConfigured = hasBuildConfigApiKey || settings.customApiKey.isNotBlank()
                        Text(
                            text = if (keyConfigured) "Status: Connected via AI Studio secrets" else "Status: Key required",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (keyConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )

                        TextButton(
                            onClick = { showApiKeyInput = !showApiKeyInput }
                        ) {
                            Text(if (showApiKeyInput) "Hide Key Field" else "Configure Custom Key")
                        }

                        if (showApiKeyInput) {
                            OutlinedTextField(
                                value = settings.customApiKey,
                                onValueChange = { settings = settings.copy(customApiKey = it) },
                                label = { Text("Custom Gemini API Key") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveSettings(settings)
                    onDismiss()
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

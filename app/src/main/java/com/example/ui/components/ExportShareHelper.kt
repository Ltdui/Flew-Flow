package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

object ExportShareHelper {

    fun copyToClipboard(context: Context, text: String, label: String = "Transcript") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Transcript copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareText(context: Context, text: String, title: String = "Share Transcript") {
        if (text.isBlank()) {
            Toast.makeText(context, "No text to share", Toast.LENGTH_SHORT).show()
            return
        }
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    fun exportAsTxt(context: Context, text: String, documentTitle: String) {
        val filename = "${documentTitle.sanitizeFilename()}.txt"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, filename)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export TXT file"))
    }

    fun exportAsMarkdown(context: Context, text: String, documentTitle: String) {
        val filename = "${documentTitle.sanitizeFilename()}.md"
        val markdownContent = buildString {
            appendLine("# $documentTitle")
            appendLine()
            appendLine("*Transcribed with Live Transcribe AI*")
            appendLine("---")
            appendLine()
            appendLine(text)
        }
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, markdownContent)
            putExtra(Intent.EXTRA_SUBJECT, filename)
            type = "text/markdown"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export Markdown file"))
    }

    private fun String.sanitizeFilename(): String {
        return this.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30).ifBlank { "transcript" }
    }
}

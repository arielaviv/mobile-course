package com.lux.field.ui.workorder.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val style = MaterialTheme.typography.bodyMedium
    val annotated = remember(text) { parseMarkdown(text) }

    Text(
        text = annotated,
        style = style,
        color = color,
        modifier = modifier,
    )
}

private fun parseMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val lines = text.lines()

    lines.forEachIndexed { lineIndex, line ->
        if (lineIndex > 0) append('\n')

        val trimmed = line.trimStart()

        // Bullet point normalization
        val bulletPrefixes = listOf("- ", "* ", "• ")
        val bulletPrefix = bulletPrefixes.firstOrNull { trimmed.startsWith(it) }

        if (bulletPrefix != null) {
            append("  • ")
            parseBoldSegments(this, trimmed.removePrefix(bulletPrefix))
        } else {
            parseBoldSegments(this, line)
        }
    }
}

private fun parseBoldSegments(builder: AnnotatedString.Builder, text: String) {
    var remaining = text
    while (remaining.isNotEmpty()) {
        val boldStart = remaining.indexOf("**")
        if (boldStart == -1) {
            builder.append(remaining)
            break
        }

        // Text before bold
        if (boldStart > 0) {
            builder.append(remaining.substring(0, boldStart))
        }

        val afterStart = remaining.substring(boldStart + 2)
        val boldEnd = afterStart.indexOf("**")
        if (boldEnd == -1) {
            // Unclosed bold — just append as-is
            builder.append(remaining.substring(boldStart))
            break
        }

        // Bold text
        val boldText = afterStart.substring(0, boldEnd)
        builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(boldText)
        }

        remaining = afterStart.substring(boldEnd + 2)
    }
}

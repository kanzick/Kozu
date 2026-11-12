package vn.kozu.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

private sealed interface ReleaseNoteBlock {
    data class Heading(val level: Int, val text: String) : ReleaseNoteBlock
    data class Paragraph(val text: String) : ReleaseNoteBlock
    data class Items(val ordered: Boolean, val values: List<String>) : ReleaseNoteBlock
    data class Quote(val text: String) : ReleaseNoteBlock
    data class Code(val text: String) : ReleaseNoteBlock
    data class Table(val rows: List<List<String>>) : ReleaseNoteBlock
    data object Divider : ReleaseNoteBlock
}

@Composable
internal fun ReleaseNotesMarkdown(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val blocks = remember(markdown) { parseReleaseNotes(markdown) }
    val annotatedBlocks = remember(blocks, colors.primary) {
        blocks.map { block ->
            when (block) {
                is ReleaseNoteBlock.Heading -> block to inlineMarkdown(block.text, colors.primary)
                is ReleaseNoteBlock.Paragraph -> block to inlineMarkdown(block.text, colors.primary)
                is ReleaseNoteBlock.Items -> block to block.values.map { inlineMarkdown(it, colors.primary) }
                is ReleaseNoteBlock.Quote -> block to inlineMarkdown(block.text, colors.primary)
                is ReleaseNoteBlock.Code -> block to inlineMarkdown(block.text, colors.primary)
                is ReleaseNoteBlock.Table -> block to block.rows.map { row -> row.map { inlineMarkdown(it, colors.primary) } }
                ReleaseNoteBlock.Divider -> block to emptyList<AnnotatedString>()
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        annotatedBlocks.forEach { (block, content) ->
            when (block) {
                is ReleaseNoteBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                    Text(
                        text = content as AnnotatedString,
                        style = style,
                        color = colors.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                is ReleaseNoteBlock.Paragraph -> {
                    Text(
                        text = content as AnnotatedString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                is ReleaseNoteBlock.Items -> {
                    val values = content as List<AnnotatedString>
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        values.forEachIndexed { index, value ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = if (block.ordered) "${index + 1}." else "•",
                                    color = colors.primary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = value,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                is ReleaseNoteBlock.Quote -> {
                    Surface(
                        color = colors.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = content as AnnotatedString,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                is ReleaseNoteBlock.Code -> {
                    Surface(
                        color = colors.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = content as AnnotatedString,
                            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                is ReleaseNoteBlock.Table -> {
                    val rows = content as List<List<AnnotatedString>>
                    Surface(
                        color = colors.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            rows.forEachIndexed { rowIndex, cells ->
                                if (rowIndex > 0) {
                                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.65f))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    cells.forEach { cell ->
                                        Text(
                                            text = cell,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant,
                                            fontWeight = if (rowIndex == 0) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                ReleaseNoteBlock.Divider -> {
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.7f))
                }
            }
        }
        Spacer(Modifier.height(1.dp))
    }
}

private fun parseReleaseNotes(markdown: String): List<ReleaseNoteBlock> {
    val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').lines()
    val result = mutableListOf<ReleaseNoteBlock>()
    val headingPattern = Regex("^#{1,6}\\s+(.+)$")
    val listPattern = Regex("^\\s*(?:([-*+])|(\\d+[.)]))\\s+(.+)$")
    val dividerPattern = Regex("^(?:-{3,}|_{3,}|\\*{3,})$")
    val tableDividerPattern = Regex("^\\s*\\|?\\s*:?-{3,}:?\\s*(?:\\|\\s*:?-{3,}:?\\s*)+\\|?\\s*$")

    fun isTable(index: Int): Boolean =
        index + 1 < lines.size && lines[index].contains('|') && tableDividerPattern.matches(lines[index + 1].trim())

    fun isBlockStart(index: Int): Boolean {
        val line = lines[index].trim()
        if (line.isEmpty()) return true
        return line.startsWith("```") ||
            headingPattern.matches(line) ||
            dividerPattern.matches(line) ||
            listPattern.matches(lines[index]) ||
            line.startsWith(">") ||
            isTable(index)
    }

    fun tableCells(line: String): List<String> =
        line.trim().removePrefix("|").removeSuffix("|").split('|').map { it.trim().replace("\\|", "|") }

    var index = 0
    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            index++
            continue
        }

        if (trimmed.startsWith("```")) {
            index++
            val codeLines = mutableListOf<String>()
            while (index < lines.size && !lines[index].trim().startsWith("```")) {
                codeLines += lines[index]
                index++
            }
            if (index < lines.size) index++
            result += ReleaseNoteBlock.Code(codeLines.joinToString("\n"))
            continue
        }

        val heading = headingPattern.matchEntire(trimmed)
        if (heading != null) {
            val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
            result += ReleaseNoteBlock.Heading(level, heading.groupValues[1].trim())
            index++
            continue
        }

        if (dividerPattern.matches(trimmed)) {
            result += ReleaseNoteBlock.Divider
            index++
            continue
        }

        if (isTable(index)) {
            val rows = mutableListOf(tableCells(line))
            index += 2
            while (index < lines.size && lines[index].contains('|') && lines[index].isNotBlank()) {
                rows += tableCells(lines[index])
                index++
            }
            result += ReleaseNoteBlock.Table(rows)
            continue
        }

        val listMatch = listPattern.matchEntire(line)
        if (listMatch != null) {
            val ordered = listMatch.groupValues[2].isNotBlank()
            val values = mutableListOf<String>()
            while (index < lines.size) {
                val item = listPattern.matchEntire(lines[index]) ?: break
                if (item.groupValues[2].isNotBlank() != ordered) break
                values += item.groupValues[3].trim()
                index++
            }
            result += ReleaseNoteBlock.Items(ordered, values)
            continue
        }

        if (trimmed.startsWith(">")) {
            val quote = mutableListOf<String>()
            while (index < lines.size && lines[index].trim().startsWith(">")) {
                quote += lines[index].trim().removePrefix(">").trim()
                index++
            }
            result += ReleaseNoteBlock.Quote(quote.joinToString(" "))
            continue
        }

        val paragraph = mutableListOf(trimmed)
        index++
        while (index < lines.size && !isBlockStart(index)) {
            paragraph += lines[index].trim()
            index++
        }
        result += ReleaseNoteBlock.Paragraph(paragraph.joinToString(" "))
    }
    return result
}

private fun inlineMarkdown(
    text: String,
    linkColor: Color,
): AnnotatedString {
    val pattern = Regex("""\[([^\]]+)]\((https?://[^)\s]+)\)|\*\*(.+?)\*\*|__(.+?)__|~~(.+?)~~|`([^`]+)`|\*(.+?)\*|_([^_]+)_""")
    return buildAnnotatedString {
        var cursor = 0
        pattern.findAll(text).forEach { match ->
            append(text.substring(cursor, match.range.first))
            val groups = match.groupValues
            when {
                groups[1].isNotEmpty() -> {
                    pushLink(
                        LinkAnnotation.Url(
                            groups[2],
                            TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                        ),
                    )
                    append(inlineMarkdown(groups[1], linkColor))
                    pop()
                }
                groups[3].isNotEmpty() || groups[4].isNotEmpty() -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
                    append(if (groups[3].isNotEmpty()) groups[3] else groups[4])
                    pop()
                }
                groups[5].isNotEmpty() -> {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(groups[5])
                    pop()
                }
                groups[6].isNotEmpty() -> {
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = linkColor.copy(alpha = 0.10f)))
                    append(groups[6])
                    pop()
                }
                groups[7].isNotEmpty() || groups[8].isNotEmpty() -> {
                    pushStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                    append(if (groups[7].isNotEmpty()) groups[7] else groups[8])
                    pop()
                }
            }
            cursor = match.range.last + 1
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
}

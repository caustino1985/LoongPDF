package com.example.parser

sealed class MarkdownElement {
    data class Heading(
        val level: Int,
        val text: String,
        val inlineSpans: List<InlineSpan> = parseInlineSpans(text)
    ) : MarkdownElement()

    data class Paragraph(
        val text: String,
        val inlineSpans: List<InlineSpan> = parseInlineSpans(text)
    ) : MarkdownElement()

    data class CodeBlock(
        val code: String,
        val language: String = ""
    ) : MarkdownElement()

    data class MermaidBlock(
        val rawCode: String,
        val diagramData: MermaidDiagramData = MermaidParser.parse(rawCode)
    ) : MarkdownElement()

    data class ListItem(
        val text: String,
        val isOrdered: Boolean = false,
        val indentLevel: Int = 0,
        val itemNumber: Int = 1,
        val inlineSpans: List<InlineSpan> = parseInlineSpans(text)
    ) : MarkdownElement()

    data class Blockquote(
        val text: String,
        val inlineSpans: List<InlineSpan> = parseInlineSpans(text)
    ) : MarkdownElement()

    object HorizontalRule : MarkdownElement()

    data class TableBlock(
        val headers: List<String>,
        val rows: List<List<String>>
    ) : MarkdownElement()

    data class ImageBlock(
        val altText: String,
        val imageUrl: String
    ) : MarkdownElement()
}

sealed class InlineSpan {
    data class NormalText(val text: String) : InlineSpan()
    data class BoldText(val text: String) : InlineSpan()
    data class ItalicText(val text: String) : InlineSpan()
    data class CodeInlineText(val text: String) : InlineSpan()
    data class LinkText(val label: String, val url: String) : InlineSpan()
}

sealed class MermaidDiagramData {
    data class Flowchart(
        val nodes: List<MermaidNode>,
        val edges: List<MermaidEdge>
    ) : MermaidDiagramData()

    data class Sequence(
        val participants: List<MermaidParticipant>,
        val messages: List<MermaidMessage>
    ) : MermaidDiagramData()

    data class Generic(val rawCode: String) : MermaidDiagramData()
}

data class MermaidNode(
    val id: String,
    val label: String,
    val shape: NodeShape = NodeShape.RECTANGLE
)

enum class NodeShape {
    RECTANGLE, ROUNDED, DIAMOND, CIRCLE
}

data class MermaidEdge(
    val fromId: String,
    val toId: String,
    val label: String = ""
)

data class MermaidParticipant(
    val id: String,
    val name: String
)

data class MermaidMessage(
    val fromId: String,
    val toId: String,
    val text: String
)

fun parseInlineSpans(text: String): List<InlineSpan> {
    val spans = mutableListOf<InlineSpan>()
    if (text.isEmpty()) return spans

    var currentText = text

    // Quick inline parsing for **bold**, *italic*, `code`, and [link](url)
    val regex = Regex("""(\*\*(.*?)\*\*|\*(.*?)\*|`(.*?)`|\[(.*?)\]\((.*?)\))""")
    var lastIndex = 0

    regex.findAll(text).forEach { match ->
        if (match.range.first > lastIndex) {
            spans.add(InlineSpan.NormalText(text.substring(lastIndex, match.range.first)))
        }

        val fullMatch = match.value
        when {
            fullMatch.startsWith("**") -> {
                val content = match.groupValues[2]
                spans.add(InlineSpan.BoldText(content))
            }
            fullMatch.startsWith("*") -> {
                val content = match.groupValues[3]
                spans.add(InlineSpan.ItalicText(content))
            }
            fullMatch.startsWith("`") -> {
                val content = match.groupValues[4]
                spans.add(InlineSpan.CodeInlineText(content))
            }
            fullMatch.startsWith("[") -> {
                val label = match.groupValues[5]
                val url = match.groupValues[6]
                spans.add(InlineSpan.LinkText(label, url))
            }
        }
        lastIndex = match.range.last + 1
    }

    if (lastIndex < text.length) {
        spans.add(InlineSpan.NormalText(text.substring(lastIndex)))
    }

    return if (spans.isEmpty()) listOf(InlineSpan.NormalText(text)) else spans
}

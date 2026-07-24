package com.example.parser

object MarkdownParser {

    fun parse(markdown: String): List<MarkdownElement> {
        val elements = mutableListOf<MarkdownElement>()
        val lines = markdown.lines()

        var i = 0
        var inCodeBlock = false
        var codeLang = ""
        val codeLines = mutableListOf<String>()

        while (i < lines.size) {
            val line = lines[i]

            // Check code block fence
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    // Close code block
                    val rawCode = codeLines.joinToString("\n")
                    if (codeLang.lowercase() == "mermaid") {
                        elements.add(MarkdownElement.MermaidBlock(rawCode))
                    } else {
                        elements.add(MarkdownElement.CodeBlock(code = rawCode, language = codeLang))
                    }
                    codeLines.clear()
                    codeLang = ""
                    inCodeBlock = false
                } else {
                    // Open code block
                    inCodeBlock = true
                    codeLang = line.trim().removePrefix("```").trim()
                }
                i++
                continue
            }

            if (inCodeBlock) {
                codeLines.add(line)
                i++
                continue
            }

            val trimmed = line.trim()

            // Skip empty line
            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // Horizontal Rule
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                elements.add(MarkdownElement.HorizontalRule)
                i++
                continue
            }

            // Headings (# to ######)
            if (trimmed.startsWith("#")) {
                val level = trimmed.takeWhile { it == '#' }.length
                if (level in 1..6 && trimmed.length > level && trimmed[level] == ' ') {
                    val headingText = trimmed.substring(level + 1).trim()
                    elements.add(MarkdownElement.Heading(level, headingText))
                    i++
                    continue
                }
            }

            // Blockquote
            if (trimmed.startsWith(">")) {
                val quoteText = trimmed.removePrefix(">").trim()
                elements.add(MarkdownElement.Blockquote(quoteText))
                i++
                continue
            }

            // Table check
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                val tableLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    tableLines.add(lines[i].trim())
                    i++
                }
                val tableElement = parseTable(tableLines)
                if (tableElement != null) {
                    elements.add(tableElement)
                } else {
                    // Fallback to paragraphs if table parsing failed
                    tableLines.forEach { elements.add(MarkdownElement.Paragraph(it)) }
                }
                continue
            }

            // Unordered list
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
                val itemText = trimmed.substring(2).trim()
                val indentLevel = (line.indexOfFirst { !it.isWhitespace() } / 2).coerceAtLeast(0)
                elements.add(MarkdownElement.ListItem(itemText, isOrdered = false, indentLevel = indentLevel))
                i++
                continue
            }

            // Ordered list
            val orderedMatch = Regex("""^\d+\.\s+(.*)$""").find(trimmed)
            if (orderedMatch != null) {
                val itemText = orderedMatch.groupValues[1].trim()
                val indentLevel = (line.indexOfFirst { !it.isWhitespace() } / 2).coerceAtLeast(0)
                elements.add(MarkdownElement.ListItem(itemText, isOrdered = true, indentLevel = indentLevel))
                i++
                continue
            }

            // Paragraph (accumulate multi-line paragraphs until blank line or special block)
            val paraLines = mutableListOf(trimmed)
            i++
            while (i < lines.size) {
                val nextTrimmed = lines[i].trim()
                if (nextTrimmed.isEmpty() || isSpecialBlockStart(nextTrimmed)) {
                    break
                }
                paraLines.add(nextTrimmed)
                i++
            }
            elements.add(MarkdownElement.Paragraph(paraLines.joinToString(" ")))
        }

        // Unclosed code block fallback
        if (inCodeBlock && codeLines.isNotEmpty()) {
            val rawCode = codeLines.joinToString("\n")
            if (codeLang.lowercase() == "mermaid") {
                elements.add(MarkdownElement.MermaidBlock(rawCode))
            } else {
                elements.add(MarkdownElement.CodeBlock(rawCode, codeLang))
            }
        }

        return elements
    }

    private fun isSpecialBlockStart(line: String): Boolean {
        return line.startsWith("#") ||
                line.startsWith("```") ||
                line.startsWith(">") ||
                line == "---" || line == "***" || line == "___" ||
                (line.startsWith("|") && line.endsWith("|")) ||
                line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") ||
                Regex("""^\d+\.\s+""").containsMatchIn(line)
    }

    private fun parseTable(tableLines: List<String>): MarkdownElement.TableBlock? {
        if (tableLines.size < 2) return null

        val parseRow = { rowStr: String ->
            rowStr.trim('|').split('|').map { it.trim() }
        }

        val headers = parseRow(tableLines[0])
        var startIndex = 1

        // Skip divider row like | --- | --- |
        if (tableLines.size > 1 && tableLines[1].contains("---")) {
            startIndex = 2
        }

        val rows = mutableListOf<List<String>>()
        for (j in startIndex until tableLines.size) {
            rows.add(parseRow(tableLines[j]))
        }

        return MarkdownElement.TableBlock(headers, rows)
    }
}

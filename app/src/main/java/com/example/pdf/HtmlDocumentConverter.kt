package com.example.pdf

import com.example.parser.MarkdownElement

object HtmlDocumentConverter {

    fun convertToHtml(
        title: String,
        elements: List<MarkdownElement>,
        config: PdfExportConfig
    ): String {
        val primaryColor = config.primaryColorHex
        val sb = StringBuilder()

        sb.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
                <style>
                    @page {
                        size: ${config.paperSize} ${if (config.isLandscape) "landscape" else "portrait"};
                        margin: ${config.marginPt}pt;
                    }
                    body {
                        font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        font-size: ${config.baseFontSize}px;
                        color: #1e293b;
                        padding: ${config.marginPt}px;
                        line-height: 1.6;
                        background-color: #ffffff;
                    }
                    h1 { color: $primaryColor; font-size: 2.2em; border-bottom: 2px solid $primaryColor; padding-bottom: 8px; margin-top: 0; }
                    h2 { color: $primaryColor; font-size: 1.6em; margin-top: 24px; }
                    h3 { color: #334155; font-size: 1.3em; margin-top: 18px; }
                    p { margin: 10px 0; }
                    code { font-family: monospace; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; color: #0f172a; }
                    pre { background: #f8fafc; border: 1px solid #cbd5e1; padding: 12px; border-radius: 8px; overflow-x: auto; }
                    pre code { background: none; padding: 0; }
                    blockquote { border-left: 4px solid $primaryColor; margin: 12px 0; padding-left: 16px; color: #475569; font-style: italic; }
                    table { border-collapse: collapse; width: 100%; margin: 16px 0; }
                    th, td { border: 1px solid #cbd5e1; padding: 8px 12px; text-align: left; }
                    th { background-color: #f1f5f9; color: #0f172a; font-weight: bold; }
                    .header-meta { font-size: 0.85em; color: #64748b; margin-bottom: 20px; }
                    .footer-meta { text-align: center; font-size: 0.8em; color: #94a3b8; margin-top: 40px; border-top: 1px solid #e2e8f0; padding-top: 12px; }
                    .mermaid-box { background: #f8fafc; border: 1px solid #bae6fd; padding: 16px; border-radius: 8px; margin: 16px 0; text-align: center; }
                    .mermaid { display: inline-block; max-width: 100%; }
                </style>
            </head>
            <body>
                <h1>${escapeHtml(title)}</h1>
                <div class="header-meta">Author: ${escapeHtml(config.author)} | Format: ${config.paperSize} (${if (config.isLandscape) "Landscape" else "Portrait"}) | Margin: ${config.marginPt.toInt()}pt</div>
        """.trimIndent())

        for (elem in elements) {
            when (elem) {
                is MarkdownElement.Heading -> {
                    when (elem.level) {
                        1 -> sb.append("<h1>${escapeHtml(elem.text)}</h1>\n")
                        2 -> sb.append("<h2>${escapeHtml(elem.text)}</h2>\n")
                        else -> sb.append("<h3>${escapeHtml(elem.text)}</h3>\n")
                    }
                }
                is MarkdownElement.Paragraph -> {
                    sb.append("<p>${escapeHtml(elem.text)}</p>\n")
                }
                is MarkdownElement.CodeBlock -> {
                    sb.append("<pre><code>${escapeHtml(elem.code)}</code></pre>\n")
                }
                is MarkdownElement.MermaidBlock -> {
                    sb.append("<div class=\"mermaid-box\"><div class=\"mermaid\">${escapeHtml(elem.rawCode)}</div></div>\n")
                }
                is MarkdownElement.ListItem -> {
                    val tag = if (elem.isOrdered) "ol" else "ul"
                    sb.append("<$tag><li>${escapeHtml(elem.text)}</li></$tag>\n")
                }
                is MarkdownElement.Blockquote -> {
                    sb.append("<blockquote>${escapeHtml(elem.text)}</blockquote>\n")
                }
                is MarkdownElement.HorizontalRule -> {
                    sb.append("<hr style=\"border: none; border-top: 1px solid #cbd5e1; margin: 20px 0;\"/>\n")
                }
                is MarkdownElement.TableBlock -> {
                    sb.append("<table><thead><tr>")
                    elem.headers.forEach { h -> sb.append("<th>${escapeHtml(h)}</th>") }
                    sb.append("</tr></thead><tbody>")
                    elem.rows.forEach { row ->
                        sb.append("<tr>")
                        row.forEach { cell -> sb.append("<td>${escapeHtml(cell)}</td>") }
                        sb.append("</tr>")
                    }
                    sb.append("</tbody></table>\n")
                }
                else -> {}
            }
        }

        if (config.footerText.isNotEmpty()) {
            sb.append("<div class=\"footer-meta\">${escapeHtml(config.footerText)}</div>\n")
        }

        sb.append("""
            <script>
                try {
                    if (typeof mermaid !== 'undefined') {
                        mermaid.initialize({ startOnLoad: true, theme: 'neutral' });
                    }
                } catch(e) {}
            </script>
            </body></html>
        """.trimIndent())
        return sb.toString()
    }

    fun convertToMergedHtml(
        bundleTitle: String,
        documentItems: List<Pair<String, List<MarkdownElement>>>,
        config: PdfExportConfig
    ): String {
        val primaryColor = config.primaryColorHex
        val sb = StringBuilder()

        sb.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
                <style>
                    @page {
                        size: ${config.paperSize} ${if (config.isLandscape) "landscape" else "portrait"};
                        margin: ${config.marginPt}pt;
                    }
                    body {
                        font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        font-size: ${config.baseFontSize}px;
                        color: #1e293b;
                        padding: ${config.marginPt}px;
                        line-height: 1.6;
                        background-color: #ffffff;
                    }
                    h1 { color: $primaryColor; font-size: 2.2em; border-bottom: 2px solid $primaryColor; padding-bottom: 8px; margin-top: 0; }
                    h2 { color: $primaryColor; font-size: 1.6em; margin-top: 24px; }
                    h3 { color: #334155; font-size: 1.3em; margin-top: 18px; }
                    p { margin: 10px 0; }
                    code { font-family: monospace; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; color: #0f172a; }
                    pre { background: #f8fafc; border: 1px solid #cbd5e1; padding: 12px; border-radius: 8px; overflow-x: auto; }
                    pre code { background: none; padding: 0; }
                    blockquote { border-left: 4px solid $primaryColor; margin: 12px 0; padding-left: 16px; color: #475569; font-style: italic; }
                    table { border-collapse: collapse; width: 100%; margin: 16px 0; }
                    th, td { border: 1px solid #cbd5e1; padding: 8px 12px; text-align: left; }
                    th { background-color: #f1f5f9; color: #0f172a; font-weight: bold; }
                    .header-meta { font-size: 0.85em; color: #64748b; margin-bottom: 20px; }
                    .footer-meta { text-align: center; font-size: 0.8em; color: #94a3b8; margin-top: 40px; border-top: 1px solid #e2e8f0; padding-top: 12px; }
                    .mermaid-box { background: #f8fafc; border: 1px solid #bae6fd; padding: 16px; border-radius: 8px; margin: 16px 0; text-align: center; }
                    .mermaid { display: inline-block; max-width: 100%; }
                    .doc-break { page-break-before: always; margin-top: 32px; padding-top: 16px; border-top: 2px dashed #cbd5e1; }
                </style>
            </head>
            <body>
                <h1>${escapeHtml(bundleTitle)}</h1>
                <div class="header-meta">Merged Collection (${documentItems.size} Documents) | Author: ${escapeHtml(config.author)} | Engine: HTML Render</div>
        """.trimIndent())

        documentItems.forEachIndexed { idx, (docTitle, elements) ->
            if (idx > 0) {
                sb.append("<div class=\"doc-break\"></div>\n")
            }
            sb.append("<h1 style=\"color: #0f172a; margin-top: 10px;\">${escapeHtml(docTitle)}</h1>\n")
            
            for (elem in elements) {
                when (elem) {
                    is MarkdownElement.Heading -> {
                        when (elem.level) {
                            1 -> sb.append("<h2>${escapeHtml(elem.text)}</h2>\n")
                            2 -> sb.append("<h3>${escapeHtml(elem.text)}</h3>\n")
                            else -> sb.append("<h4>${escapeHtml(elem.text)}</h4>\n")
                        }
                    }
                    is MarkdownElement.Paragraph -> {
                        sb.append("<p>${escapeHtml(elem.text)}</p>\n")
                    }
                    is MarkdownElement.CodeBlock -> {
                        sb.append("<pre><code>${escapeHtml(elem.code)}</code></pre>\n")
                    }
                    is MarkdownElement.MermaidBlock -> {
                        sb.append("<div class=\"mermaid-box\"><div class=\"mermaid\">${escapeHtml(elem.rawCode)}</div></div>\n")
                    }
                    is MarkdownElement.ListItem -> {
                        val tag = if (elem.isOrdered) "ol" else "ul"
                        sb.append("<$tag><li>${escapeHtml(elem.text)}</li></$tag>\n")
                    }
                    is MarkdownElement.Blockquote -> {
                        sb.append("<blockquote>${escapeHtml(elem.text)}</blockquote>\n")
                    }
                    is MarkdownElement.HorizontalRule -> {
                        sb.append("<hr style=\"border: none; border-top: 1px solid #cbd5e1; margin: 20px 0;\"/>\n")
                    }
                    is MarkdownElement.TableBlock -> {
                        sb.append("<table><thead><tr>")
                        elem.headers.forEach { h -> sb.append("<th>${escapeHtml(h)}</th>") }
                        sb.append("</tr></thead><tbody>")
                        elem.rows.forEach { row ->
                            sb.append("<tr>")
                            row.forEach { cell -> sb.append("<td>${escapeHtml(cell)}</td>") }
                            sb.append("</tr>")
                        }
                        sb.append("</tbody></table>\n")
                    }
                    else -> {}
                }
            }
        }

        if (config.footerText.isNotEmpty()) {
            sb.append("<div class=\"footer-meta\">${escapeHtml(config.footerText)}</div>\n")
        }

        sb.append("""
            <script>
                try {
                    if (typeof mermaid !== 'undefined') {
                        mermaid.initialize({ startOnLoad: true, theme: 'neutral' });
                    }
                } catch(e) {}
            </script>
            </body></html>
        """.trimIndent())
        return sb.toString()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}


package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.parser.InlineSpan
import com.example.parser.MarkdownElement
import com.example.parser.MermaidDiagramData
import com.example.parser.NodeShape
import java.io.File
import java.io.FileOutputStream

object PdfPageRenderer {

    fun generatePdf(
        context: Context,
        title: String,
        elements: List<MarkdownElement>,
        config: PdfExportConfig
    ): File {
        val (pageWidth, pageHeight) = config.getDimensions()
        val pdfDocument = PdfDocument()

        val margin = config.marginPt
        val contentWidth = pageWidth - (margin * 2)
        val maxY = pageHeight - margin - 30f // Reserve space for footer

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var yPos = margin + 20f

        // Paints
        val primaryColor = config.parsePrimaryColor()

        val titlePaint = Paint().apply {
            color = primaryColor
            textSize = config.baseFontSize * 2.2f
            isFakeBoldText = true
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val h1Paint = Paint().apply {
            color = primaryColor
            textSize = config.baseFontSize * 1.6f
            isFakeBoldText = true
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val h2Paint = Paint().apply {
            color = primaryColor
            textSize = config.baseFontSize * 1.35f
            isFakeBoldText = true
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val h3Paint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = config.baseFontSize * 1.15f
            isFakeBoldText = true
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = config.baseFontSize
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }

        val boldPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = config.baseFontSize
            isFakeBoldText = true
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val italicPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = config.baseFontSize
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        val codePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = config.baseFontSize * 0.9f
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
        }

        val metaPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = config.baseFontSize * 0.85f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val bgBoxPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }

        val quoteBorderPaint = Paint().apply {
            color = primaryColor
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }

        fun drawHeaderFooter(c: Canvas, pNum: Int) {
            if (config.headerText.isNotEmpty()) {
                c.drawText(config.headerText, margin, margin - 10f, metaPaint)
                c.drawLine(margin, margin - 5f, pageWidth - margin, margin - 5f, linePaint)
            }

            val footerY = pageHeight - margin + 15f
            c.drawLine(margin, footerY - 10f, pageWidth - margin, footerY - 10f, linePaint)
            c.drawText(config.footerText, margin, footerY, metaPaint)

            if (config.showPageNumbers) {
                val pageText = "Page $pNum"
                val pWidth = metaPaint.measureText(pageText)
                c.drawText(pageText, pageWidth - margin - pWidth, footerY, metaPaint)
            }
        }

        fun checkAndNextPage(requiredHeight: Float) {
            if (yPos + requiredHeight > maxY) {
                drawHeaderFooter(canvas, pageNum)
                pdfDocument.finishPage(page)

                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = margin + 20f
            }
        }

        // Draw Document Header
        checkAndNextPage(60f)
        canvas.drawText(title, margin, yPos, titlePaint)
        yPos += config.baseFontSize * 2.5f

        val authorMeta = "Author: ${config.author} | Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}"
        canvas.drawText(authorMeta, margin, yPos, metaPaint)
        yPos += config.baseFontSize * 1.5f
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        yPos += 20f

        // Render AST Elements
        for (elem in elements) {
            when (elem) {
                is MarkdownElement.Heading -> {
                    val p = when (elem.level) {
                        1 -> h1Paint
                        2 -> h2Paint
                        else -> h3Paint
                    }
                    val hSpacing = p.textSize * 1.6f
                    checkAndNextPage(hSpacing + 10f)
                    canvas.drawText(elem.text, margin, yPos + p.textSize, p)
                    yPos += hSpacing + 12f
                }

                is MarkdownElement.Paragraph -> {
                    val lines = wrapText(elem.text, bodyPaint, contentWidth)
                    val lineH = config.baseFontSize * 1.4f
                    checkAndNextPage(lines.size * lineH + 10f)

                    lines.forEach { lineText ->
                        canvas.drawText(lineText, margin, yPos, bodyPaint)
                        yPos += lineH
                    }
                    yPos += 8f
                }

                is MarkdownElement.CodeBlock -> {
                    val codeLines = elem.code.lines()
                    val lineH = config.baseFontSize * 1.3f
                    val boxH = (codeLines.size * lineH) + 16f

                    checkAndNextPage(boxH + 10f)

                    // Draw grey background box
                    val rect = RectF(margin, yPos, margin + contentWidth, yPos + boxH)
                    canvas.drawRoundRect(rect, 8f, 8f, bgBoxPaint)

                    var codeY = yPos + lineH + 4f
                    codeLines.forEach { cLine ->
                        canvas.drawText(cLine, margin + 12f, codeY, codePaint)
                        codeY += lineH
                    }

                    yPos += boxH + 12f
                }

                is MarkdownElement.MermaidBlock -> {
                    val diagramH = 180f
                    checkAndNextPage(diagramH + 20f)

                    // Draw Diagram Container
                    val rect = RectF(margin, yPos, margin + contentWidth, yPos + diagramH)
                    canvas.drawRoundRect(rect, 10f, 10f, bgBoxPaint)
                    canvas.drawRoundRect(rect, 10f, 10f, linePaint)

                    drawMermaidOnCanvas(canvas, rect, elem.diagramData, primaryColor)

                    yPos += diagramH + 16f
                }

                is MarkdownElement.ListItem -> {
                    val prefix = if (elem.isOrdered) "${elem.itemNumber}. " else "• "
                    val indent = margin + (elem.indentLevel * 15f)
                    val lines = wrapText("$prefix${elem.text}", bodyPaint, contentWidth - (elem.indentLevel * 15f))
                    val lineH = config.baseFontSize * 1.4f

                    checkAndNextPage(lines.size * lineH + 4f)

                    lines.forEach { lineText ->
                        canvas.drawText(lineText, indent, yPos, bodyPaint)
                        yPos += lineH
                    }
                    yPos += 4f
                }

                is MarkdownElement.Blockquote -> {
                    val lines = wrapText(elem.text, italicPaint, contentWidth - 20f)
                    val lineH = config.baseFontSize * 1.4f
                    val qH = lines.size * lineH + 8f

                    checkAndNextPage(qH + 8f)

                    canvas.drawLine(margin + 4f, yPos, margin + 4f, yPos + qH, quoteBorderPaint)

                    var qY = yPos + config.baseFontSize
                    lines.forEach { lText ->
                        canvas.drawText(lText, margin + 16f, qY, italicPaint)
                        qY += lineH
                    }

                    yPos += qH + 12f
                }

                is MarkdownElement.HorizontalRule -> {
                    checkAndNextPage(15f)
                    canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
                    yPos += 15f
                }

                is MarkdownElement.TableBlock -> {
                    val cols = elem.headers.size.coerceAtLeast(1)
                    val colWidth = contentWidth / cols
                    val rowH = config.baseFontSize * 1.8f
                    val tableH = (elem.rows.size + 1) * rowH

                    checkAndNextPage(tableH + 15f)

                    // Header row bg
                    val headerRect = RectF(margin, yPos, margin + contentWidth, yPos + rowH)
                    val headerBg = Paint().apply {
                        color = Color.parseColor("#E2E8F0")
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(headerRect, headerBg)

                    // Draw Header Text
                    elem.headers.forEachIndexed { idx, hText ->
                        val x = margin + (idx * colWidth) + 8f
                        canvas.drawText(hText.take(15), x, yPos + (rowH * 0.65f), boldPaint)
                    }

                    yPos += rowH

                    // Draw Rows
                    elem.rows.forEach { row ->
                        row.forEachIndexed { idx, cellText ->
                            val x = margin + (idx * colWidth) + 8f
                            canvas.drawText(cellText.take(15), x, yPos + (rowH * 0.65f), bodyPaint)
                        }
                        canvas.drawLine(margin, yPos, margin + contentWidth, yPos, linePaint)
                        yPos += rowH
                    }

                    canvas.drawRect(RectF(margin, yPos - tableH, margin + contentWidth, yPos), linePaint)
                    yPos += 12f
                }

                else -> {}
            }
        }

        // Finish Last Page
        drawHeaderFooter(canvas, pageNum)
        pdfDocument.finishPage(page)

        // Write file
        val outputDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "documents")
        if (!outputDir.exists()) outputDir.mkdirs()

        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
        val pdfFile = File(outputDir, "${sanitizedTitle}_${System.currentTimeMillis()}.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines.ifEmpty { listOf(text) }
    }

    private fun drawMermaidOnCanvas(
        canvas: Canvas,
        rect: RectF,
        diagramData: MermaidDiagramData,
        primaryColorInt: Int
    ) {
        val nodePaint = Paint().apply {
            color = Color.parseColor("#E0F2FE")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val nodeBorderPaint = Paint().apply {
            color = primaryColorInt
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            isAntiAlias = true
            isFakeBoldText = true
            typeface = Typeface.DEFAULT_BOLD
        }

        val arrowPaint = Paint().apply {
            color = primaryColorInt
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        when (diagramData) {
            is MermaidDiagramData.Flowchart -> {
                val nodes = diagramData.nodes
                val edges = diagramData.edges
                if (nodes.isEmpty()) return

                val nodePositions = mutableMapOf<String, Pair<Float, Float>>()
                val startY = rect.top + 30f
                val spacingY = 45f
                val centerX = rect.centerX()

                nodes.forEachIndexed { idx, node ->
                    val x = if (idx % 2 == 0) centerX - 60f else centerX + 60f
                    val y = startY + (idx * spacingY)
                    if (y < rect.bottom - 20f) {
                        nodePositions[node.id] = Pair(x, y)

                        val nodeW = 100f
                        val nodeH = 32f
                        val nodeRect = RectF(x - (nodeW / 2), y - (nodeH / 2), x + (nodeW / 2), y + (nodeH / 2))

                        when (node.shape) {
                            NodeShape.ROUNDED -> canvas.drawRoundRect(nodeRect, 16f, 16f, nodePaint)
                            NodeShape.DIAMOND -> {
                                val path = Path().apply {
                                    moveTo(x, y - (nodeH / 2))
                                    lineTo(x + (nodeW / 2), y)
                                    lineTo(x, y + (nodeH / 2))
                                    lineTo(x - (nodeW / 2), y)
                                    close()
                                }
                                canvas.drawPath(path, nodePaint)
                                canvas.drawPath(path, nodeBorderPaint)
                            }
                            else -> canvas.drawRoundRect(nodeRect, 6f, 6f, nodePaint)
                        }

                        if (node.shape != NodeShape.DIAMOND) {
                            canvas.drawRoundRect(nodeRect, 6f, 6f, nodeBorderPaint)
                        }

                        val labelText = node.label.take(14)
                        val tWidth = textPaint.measureText(labelText)
                        canvas.drawText(labelText, x - (tWidth / 2), y + 3.5f, textPaint)
                    }
                }

                // Draw Edges
                edges.forEach { edge ->
                    val p1 = nodePositions[edge.fromId]
                    val p2 = nodePositions[edge.toId]
                    if (p1 != null && p2 != null) {
                        canvas.drawLine(p1.first, p1.second + 16f, p2.first, p2.second - 16f, arrowPaint)
                    }
                }
            }

            is MermaidDiagramData.Sequence -> {
                val participants = diagramData.participants
                if (participants.isEmpty()) return

                val count = participants.size.coerceAtMost(4)
                val colW = rect.width() / (count + 1)

                val pXMap = mutableMapOf<String, Float>()
                participants.take(count).forEachIndexed { idx, p ->
                    val x = rect.left + ((idx + 1) * colW)
                    pXMap[p.id] = x

                    val pRect = RectF(x - 35f, rect.top + 15f, x + 35f, rect.top + 45f)
                    canvas.drawRoundRect(pRect, 6f, 6f, nodePaint)
                    canvas.drawRoundRect(pRect, 6f, 6f, nodeBorderPaint)

                    val label = p.name.take(10)
                    val tWidth = textPaint.measureText(label)
                    canvas.drawText(label, x - (tWidth / 2), rect.top + 33f, textPaint)

                    // Lifeline
                    canvas.drawLine(x, rect.top + 45f, x, rect.bottom - 15f, arrowPaint)
                }

                var msgY = rect.top + 65f
                diagramData.messages.take(3).forEach { msg ->
                    val x1 = pXMap[msg.fromId]
                    val x2 = pXMap[msg.toId]
                    if (x1 != null && x2 != null && msgY < rect.bottom - 20f) {
                        canvas.drawLine(x1, msgY, x2, msgY, arrowPaint)
                        canvas.drawText(msg.text.take(12), (x1 + x2) / 2 - 20f, msgY - 4f, textPaint)
                        msgY += 30f
                    }
                }
            }

            else -> {
                val label = "Mermaid Diagram Code Block"
                canvas.drawText(label, rect.centerX() - 60f, rect.centerY(), textPaint)
            }
        }
    }
}

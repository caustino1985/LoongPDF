package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parser.InlineSpan
import com.example.parser.MarkdownElement
import com.example.pdf.PdfExportConfig

@Composable
fun PdfDocumentPagePreview(
    title: String,
    elements: List<MarkdownElement>,
    config: PdfExportConfig,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Simulated Paper Sheet Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(4.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding((config.marginPt * 0.6f).dp.coerceIn(12.dp, 36.dp))
            ) {
                // Header Line
                if (config.headerText.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = config.headerText,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "${config.paperSize} • ${if (config.isLandscape) "Landscape" else "Portrait"}",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)
                }

                // Document Title Banner
                Text(
                    text = title.ifEmpty { "Untitled Document" },
                    fontSize = (config.baseFontSize * 1.8f).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(config.parsePrimaryColor())
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Author: ${config.author}",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "Format: ${config.paperSize} (${if (config.isLandscape) "Landscape" else "Portrait"}) | ${config.marginPt.toInt()}pt Margin",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }

                HorizontalDivider(color = Color(config.parsePrimaryColor()), thickness = 2.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Render AST Elements
                elements.forEach { elem ->
                    RenderElementPreview(elem = elem, config = config)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray)

                // Footer Line
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = config.footerText,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    if (config.showPageNumbers) {
                        Text(
                            text = "Page 1 of 1",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RenderElementPreview(elem: MarkdownElement, config: PdfExportConfig) {
    val primaryColor = Color(config.parsePrimaryColor())

    when (elem) {
        is MarkdownElement.Heading -> {
            val fontSize = when (elem.level) {
                1 -> (config.baseFontSize * 1.6f).sp
                2 -> (config.baseFontSize * 1.35f).sp
                else -> (config.baseFontSize * 1.15f).sp
            }
            Text(
                text = buildInlineAnnotatedString(elem.inlineSpans),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = if (elem.level <= 2) primaryColor else Color(0xFF334155),
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        is MarkdownElement.Paragraph -> {
            Text(
                text = buildInlineAnnotatedString(elem.inlineSpans),
                fontSize = config.baseFontSize.sp,
                color = Color(0xFF1E293B),
                lineHeight = (config.baseFontSize * 1.4f).sp
            )
        }

        is MarkdownElement.CodeBlock -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp)),
                color = Color(0xFFF1F5F9),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (elem.language.isNotEmpty()) {
                        Text(
                            text = elem.language.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text = elem.code,
                        fontFamily = FontFamily.Monospace,
                        fontSize = (config.baseFontSize * 0.85f).sp,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        }

        is MarkdownElement.MermaidBlock -> {
            MermaidDiagramComposable(
                diagramData = elem.diagramData,
                rawCode = elem.rawCode
            )
        }

        is MarkdownElement.ListItem -> {
            Row(modifier = Modifier.padding(start = (elem.indentLevel * 12).dp)) {
                Text(
                    text = if (elem.isOrdered) "${elem.itemNumber}. " else "• ",
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    fontSize = config.baseFontSize.sp
                )
                Text(
                    text = buildInlineAnnotatedString(elem.inlineSpans),
                    fontSize = config.baseFontSize.sp,
                    color = Color(0xFF1E293B)
                )
            }
        }

        is MarkdownElement.Blockquote -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .background(primaryColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildInlineAnnotatedString(elem.inlineSpans),
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF475569),
                    fontSize = config.baseFontSize.sp
                )
            }
        }

        is MarkdownElement.HorizontalRule -> {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)
        }

        is MarkdownElement.TableBlock -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Column {
                    // Headers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE2E8F0))
                            .padding(8.dp)
                    ) {
                        elem.headers.forEach { hText ->
                            Text(
                                text = hText,
                                fontWeight = FontWeight.Bold,
                                fontSize = (config.baseFontSize * 0.9f).sp,
                                modifier = Modifier.weight(1f),
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    // Rows
                    elem.rows.forEach { row ->
                        HorizontalDivider(color = Color.LightGray)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            row.forEach { cellText ->
                                Text(
                                    text = cellText,
                                    fontSize = (config.baseFontSize * 0.85f).sp,
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    }
                }
            }
        }

        else -> {}
    }
}

fun buildInlineAnnotatedString(spans: List<InlineSpan>): AnnotatedString {
    return buildAnnotatedString {
        spans.forEach { span ->
            when (span) {
                is InlineSpan.NormalText -> append(span.text)
                is InlineSpan.BoldText -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(span.text)
                    pop()
                }
                is InlineSpan.ItalicText -> {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(span.text)
                    pop()
                }
                is InlineSpan.CodeInlineText -> {
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFFE2E8F0)))
                    append(" ${span.text} ")
                    pop()
                }
                is InlineSpan.LinkText -> {
                    pushStyle(SpanStyle(color = Color(0xFF2563EB), textDecoration = TextDecoration.Underline))
                    append(span.label)
                    pop()
                }
            }
        }
    }
}

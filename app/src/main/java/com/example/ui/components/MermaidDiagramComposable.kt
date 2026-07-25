package com.example.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.parser.MermaidDiagramData
import com.example.parser.MermaidEdge
import com.example.parser.MermaidNode
import com.example.parser.NodeShape

@Composable
fun MermaidDiagramComposable(
    diagramData: MermaidDiagramData,
    rawCode: String = "",
    modifier: Modifier = Modifier
) {
    var selectedInfo by remember { mutableStateOf<String?>(null) }
    var renderMode by remember { mutableIntStateOf(0) } // 0 = Mermaid.js Web Engine, 1 = Native Vector

    val codeToRender = if (rawCode.isNotBlank()) rawCode else when (diagramData) {
        is MermaidDiagramData.Generic -> diagramData.rawCode
        else -> ""
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Diagram Header Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = "Mermaid Diagram",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = when (diagramData) {
                            is MermaidDiagramData.Flowchart -> "Mermaid Flowchart"
                            is MermaidDiagramData.Sequence -> "Mermaid Sequence"
                            else -> "Mermaid.js Diagram"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (renderMode == 0) "Mermaid.js" else "Native Vector",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Render Mode Segmented Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = renderMode == 0,
                        onClick = { renderMode = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Mermaid.js", fontSize = 11.sp)
                    }
                    SegmentedButton(
                        selected = renderMode == 1,
                        onClick = { renderMode = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Native Vector", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (renderMode == 0 && codeToRender.isNotBlank()) {
                // Mermaid.js Official JS Engine via WebView
                MermaidWebViewRender(rawCode = codeToRender)
            } else {
                // Native Compose Vector Renderer Fallback
                when (diagramData) {
                    is MermaidDiagramData.Flowchart -> {
                        FlowchartComposable(
                            nodes = diagramData.nodes,
                            edges = diagramData.edges,
                            onNodeSelect = { selectedInfo = "Node [${it.id}]: ${it.label}" }
                        )
                    }
                    is MermaidDiagramData.Sequence -> {
                        SequenceComposable(
                            participants = diagramData.participants,
                            messages = diagramData.messages,
                            onMessageSelect = { selectedInfo = "Message: ${it.text} (${it.fromId} -> ${it.toId})" }
                        )
                    }
                    else -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = codeToRender,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            if (selectedInfo != null && renderMode == 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = selectedInfo!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MermaidWebViewRender(
    rawCode: String,
    modifier: Modifier = Modifier
) {
    val cleanCode = remember(rawCode) {
        rawCode.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    val htmlContent = remember(cleanCode) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8"/>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
            <style>
                body {
                    margin: 0;
                    padding: 12px;
                    background-color: transparent;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    font-family: system-ui, -apple-system, sans-serif;
                }
                .mermaid {
                    width: 100%;
                    text-align: center;
                }
                svg {
                    max-width: 100% !important;
                    height: auto !important;
                }
            </style>
        </head>
        <body>
            <div class="mermaid">
                $cleanCode
            </div>
            <script>
                try {
                    mermaid.initialize({
                        startOnLoad: true,
                        theme: 'neutral',
                        securityLevel: 'loose'
                    });
                } catch(e) {
                    console.error(e);
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = object : WebViewClient() {}
                loadDataWithBaseURL("https://cdn.jsdelivr.net", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://cdn.jsdelivr.net", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 400.dp)
            .clip(RoundedCornerShape(8.dp))
    )
}

@Composable
fun FlowchartComposable(
    nodes: List<MermaidNode>,
    edges: List<MermaidEdge>,
    onNodeSelect: (MermaidNode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        nodes.forEachIndexed { index, node ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val shapeModifier = when (node.shape) {
                    NodeShape.ROUNDED -> RoundedCornerShape(20.dp)
                    NodeShape.DIAMOND -> RoundedCornerShape(4.dp)
                    else -> RoundedCornerShape(8.dp)
                }

                Surface(
                    shape = shapeModifier,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .clickable { onNodeSelect(node) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = node.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Edge Connector Arrow if not last node
            if (index < nodes.size - 1) {
                val matchingEdge = edges.find { it.fromId == node.id }
                val edgeLabel = matchingEdge?.label ?: ""

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (edgeLabel.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = edgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Arrow Line Canvas
                    val arrowColor = MaterialTheme.colorScheme.primary
                    Canvas(
                        modifier = Modifier
                            .width(20.dp)
                            .height(24.dp)
                    ) {
                        drawLine(
                            color = arrowColor,
                            start = Offset(size.width / 2, 0f),
                            end = Offset(size.width / 2, size.height),
                            strokeWidth = 3f
                        )
                        val path = Path().apply {
                            moveTo(size.width / 2, size.height)
                            lineTo(size.width / 2 - 6f, size.height - 8f)
                            lineTo(size.width / 2 + 6f, size.height - 8f)
                            close()
                        }
                        drawPath(path, arrowColor)
                    }
                }
            }
        }
    }
}

@Composable
fun SequenceComposable(
    participants: List<com.example.parser.MermaidParticipant>,
    messages: List<com.example.parser.MermaidMessage>,
    onMessageSelect: (com.example.parser.MermaidMessage) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Participants Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            participants.forEach { participant ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Text(
                        text = participant.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Message Items List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            messages.forEach { msg ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMessageSelect(msg) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${msg.fromId} → ${msg.toId}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

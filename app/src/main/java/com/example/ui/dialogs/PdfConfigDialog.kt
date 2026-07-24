package com.example.ui.dialogs

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pdf.PdfExportConfig
import com.example.ui.viewmodel.PdfEngine

@Composable
fun PdfConfigDialog(
    initialConfig: PdfExportConfig,
    currentEngine: PdfEngine,
    onEngineChanged: (PdfEngine) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (PdfExportConfig) -> Unit
) {
    var paperSize by remember { mutableStateOf(initialConfig.paperSize) }
    var isLandscape by remember { mutableStateOf(initialConfig.isLandscape) }
    var marginPt by remember { mutableFloatStateOf(initialConfig.marginPt) }
    var selectedColorHex by remember { mutableStateOf(initialConfig.primaryColorHex) }
    var baseFontSize by remember { mutableFloatStateOf(initialConfig.baseFontSize) }
    var author by remember { mutableStateOf(initialConfig.author) }
    var headerText by remember { mutableStateOf(initialConfig.headerText) }
    var footerText by remember { mutableStateOf(initialConfig.footerText) }
    var engine by remember { mutableStateOf(currentEngine) }

    val colorOptions = listOf(
        "#0F172A", // Dark Slate
        "#1E3A8A", // Deep Navy
        "#065F46", // Emerald
        "#7C3AED", // Royal Purple
        "#831843"  // Rose Velvet
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("PDF Style & Page Settings")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PDF Engine Selection
                Column {
                    Text("PDF Generation Engine", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = engine == PdfEngine.NATIVE_VECTOR,
                            onClick = {
                                engine = PdfEngine.NATIVE_VECTOR
                                onEngineChanged(PdfEngine.NATIVE_VECTOR)
                            },
                            label = { Text("Native (PdfDocument)") }
                        )
                        FilterChip(
                            selected = engine == PdfEngine.WEBVIEW_HTML,
                            onClick = {
                                engine = PdfEngine.WEBVIEW_HTML
                                onEngineChanged(PdfEngine.WEBVIEW_HTML)
                            },
                            label = { Text("WebView HTML Print") }
                        )
                    }
                }

                // Page Orientation
                Column {
                    Text("Page Orientation", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isLandscape,
                            onClick = { isLandscape = false },
                            label = { Text("Portrait (Vertical)") }
                        )
                        FilterChip(
                            selected = isLandscape,
                            onClick = { isLandscape = true },
                            label = { Text("Landscape (Horizontal)") }
                        )
                    }
                }

                // Paper Size Selection
                Column {
                    Text("Paper Size", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("A4", "Letter", "A3", "A5").forEach { size ->
                            FilterChip(
                                selected = paperSize == size,
                                onClick = { paperSize = size },
                                label = { Text(size) }
                            )
                        }
                    }
                }

                // Page Margins Selection
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Page Margins", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("${marginPt.toInt()} pt", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Narrow" to 20f,
                            "Normal" to 40f,
                            "Wide" to 60f
                        ).forEach { (label, pt) ->
                            FilterChip(
                                selected = marginPt == pt,
                                onClick = { marginPt = pt },
                                label = { Text(label) }
                            )
                        }
                    }
                    Slider(
                        value = marginPt,
                        onValueChange = { marginPt = it },
                        valueRange = 10f..80f,
                        steps = 13
                    )
                }

                // Primary Color Selection
                Column {
                    Text("Primary Brand Color", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        colorOptions.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedColorHex == hex) 3.dp else 1.dp,
                                        color = if (selectedColorHex == hex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            ) {
                                if (selectedColorHex == hex) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Base Font Size Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Base Font Size", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("${baseFontSize.toInt()} pt", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = baseFontSize,
                        onValueChange = { baseFontSize = it },
                        valueRange = 9f..16f,
                        steps = 7
                    )
                }

                // Author Input
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author / Publisher") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Header Text Input
                OutlinedTextField(
                    value = headerText,
                    onValueChange = { headerText = it },
                    label = { Text("Page Header Text (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Footer Text Input
                OutlinedTextField(
                    value = footerText,
                    onValueChange = { footerText = it },
                    label = { Text("Page Footer Text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        initialConfig.copy(
                            paperSize = paperSize,
                            isLandscape = isLandscape,
                            marginPt = marginPt,
                            primaryColorHex = selectedColorHex,
                            baseFontSize = baseFontSize,
                            author = author,
                            headerText = headerText,
                            footerText = footerText
                        )
                    )
                }
            ) {
                Text("Apply & Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

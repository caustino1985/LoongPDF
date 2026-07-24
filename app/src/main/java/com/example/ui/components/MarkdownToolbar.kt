package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownToolbar(
    onInsertText: (prefix: String, suffix: String) -> Unit,
    onOpenOcrScanner: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onOpenOcrScanner != null) {
                // OCR Camera & Image Scan
                AssistChip(
                    onClick = { onOpenOcrScanner() },
                    label = { Text("Camera OCR", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.DocumentScanner, contentDescription = "OCR Scan", tint = MaterialTheme.colorScheme.primary) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            // Heading 1
            AssistChip(
                onClick = { onInsertText("# ", "") },
                label = { Text("H1", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = "H1") }
            )

            // Heading 2
            AssistChip(
                onClick = { onInsertText("## ", "") },
                label = { Text("H2") }
            )

            // Bold
            AssistChip(
                onClick = { onInsertText("**", "**") },
                label = { Text("Bold") },
                leadingIcon = { Icon(Icons.Default.FormatBold, contentDescription = "Bold") }
            )

            // Italic
            AssistChip(
                onClick = { onInsertText("*", "*") },
                label = { Text("Italic") },
                leadingIcon = { Icon(Icons.Default.FormatItalic, contentDescription = "Italic") }
            )

            // Code Inline
            AssistChip(
                onClick = { onInsertText("`", "`") },
                label = { Text("Code") },
                leadingIcon = { Icon(Icons.Default.Code, contentDescription = "Code") }
            )

            // Code Block
            AssistChip(
                onClick = { onInsertText("```kotlin\n", "\n```") },
                label = { Text("Block") }
            )

            // Mermaid Flowchart
            AssistChip(
                onClick = {
                    onInsertText(
                        "```mermaid\nflowchart TD\n    A[Start] --> B{Decision?}\n    B -->|Yes| C[Process]\n    B -->|No| D[End]\n```\n",
                        ""
                    )
                },
                label = { Text("Flowchart", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )

            // Mermaid Sequence
            AssistChip(
                onClick = {
                    onInsertText(
                        "```mermaid\nsequenceDiagram\n    participant User\n    participant Server\n    User->>Server: Request PDF\n    Server-->>User: Return PDF File\n```\n",
                        ""
                    )
                },
                label = { Text("Sequence", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )

            // Bullet List
            AssistChip(
                onClick = { onInsertText("- ", "") },
                label = { Text("List") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "List") }
            )

            // Numbered List
            AssistChip(
                onClick = { onInsertText("1. ", "") },
                label = { Text("Numbers") },
                leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = "Ordered List") }
            )

            // Blockquote
            AssistChip(
                onClick = { onInsertText("> ", "") },
                label = { Text("Quote") },
                leadingIcon = { Icon(Icons.Default.FormatQuote, contentDescription = "Quote") }
            )

            // Table
            AssistChip(
                onClick = {
                    onInsertText(
                        "\n| Column 1 | Column 2 |\n| :--- | :--- |\n| Data A | Data B |\n",
                        ""
                    )
                },
                label = { Text("Table") },
                leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = "Table") }
            )

            // Divider Line
            AssistChip(
                onClick = { onInsertText("\n---\n", "") },
                label = { Text("HR") },
                leadingIcon = { Icon(Icons.Default.HorizontalRule, contentDescription = "Divider") }
            )
        }
    }
}

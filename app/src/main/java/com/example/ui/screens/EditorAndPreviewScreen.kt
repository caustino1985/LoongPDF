package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pdf.PdfShareManager
import com.example.ui.components.MarkdownToolbar
import com.example.ui.components.PdfDocumentPagePreview
import com.example.ui.dialogs.PdfConfigDialog
import com.example.ui.viewmodel.DocumentViewModel
import com.example.ui.viewmodel.PdfEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorAndPreviewScreen(
    viewModel: DocumentViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activeDoc by viewModel.activeDocument.collectAsStateWithLifecycle()
    val parsedElements by viewModel.parsedElements.collectAsStateWithLifecycle()
    val pdfConfig by viewModel.pdfConfig.collectAsStateWithLifecycle()
    val pdfEngine by viewModel.pdfEngine.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val lastExportedFile by viewModel.lastExportedFile.collectAsStateWithLifecycle()

    if (activeDoc == null) {
        onBack()
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Editor, 1 = PDF Sheet Preview
    var showConfigDialog by remember { mutableStateOf(false) }

    // SAF Create Document Launcher (Pick folder/file destination)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            viewModel.exportToCustomUri(context, uri) { success ->
                if (success) {
                    Toast.makeText(context, "PDF saved to selected destination!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to save PDF to selected location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Text field state with selection management
    var editorTextState by remember(activeDoc!!.id) {
        mutableStateOf(TextFieldValue(activeDoc!!.content))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = activeDoc!!.title,
                        onValueChange = { viewModel.updateActiveTitle(it) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showConfigDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "PDF Page Config")
                    }

                    // Save to custom folder / location via SAF
                    IconButton(
                        onClick = {
                            val defaultName = "${activeDoc!!.title.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()}.pdf"
                            createDocumentLauncher.launch(defaultName)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Select Folder & Save PDF",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Quick Export to App Dir
                    IconButton(
                        onClick = {
                            viewModel.exportToPdf(context) { file ->
                                Toast.makeText(context, "PDF Exported: ${file.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp).width(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Quick Export PDF",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (lastExportedFile != null && lastExportedFile!!.exists()) {
                        IconButton(onClick = { PdfShareManager.sharePdf(context, lastExportedFile!!) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share PDF")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // View Mode Switcher Row
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        ) {
                            Text("Markdown Editor")
                        }

                        SegmentedButton(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
                        ) {
                            Text("Live PDF Sheet")
                        }
                    }
                }
            }

            // Workspace Content Body
            when (selectedTab) {
                0 -> {
                    // Markdown Editor Mode
                    Column(modifier = Modifier.fillMaxSize()) {
                        MarkdownToolbar(
                            onInsertText = { prefix, suffix ->
                                val sel = editorTextState.selection
                                val text = editorTextState.text
                                val before = text.substring(0, sel.min)
                                val selected = text.substring(sel.min, sel.max)
                                val after = text.substring(sel.max)

                                val newText = before + prefix + selected + suffix + after
                                val newCursorPos = sel.min + prefix.length + selected.length + suffix.length

                                editorTextState = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newCursorPos)
                                )
                                viewModel.updateActiveContent(newText)
                            }
                        )

                        // Code Editor Input Area
                        TextField(
                            value = editorTextState,
                            onValueChange = {
                                editorTextState = it
                                viewModel.updateActiveContent(it.text)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            ),
                            placeholder = { Text("Write Markdown and Mermaid diagram syntax...") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }

                1 -> {
                    // Live PDF Page Simulation Mode
                    PdfDocumentPagePreview(
                        title = activeDoc!!.title,
                        elements = parsedElements,
                        config = pdfConfig,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showConfigDialog) {
        PdfConfigDialog(
            initialConfig = pdfConfig,
            currentEngine = pdfEngine,
            onEngineChanged = { viewModel.setPdfEngine(it) },
            onDismiss = { showConfigDialog = false },
            onConfirm = { updatedConfig ->
                viewModel.updatePdfConfig(updatedConfig)
                showConfigDialog = false
            }
        )
    }
}

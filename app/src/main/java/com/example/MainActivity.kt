package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.DocumentListScreen
import com.example.ui.screens.EditorAndPreviewScreen
import com.example.ui.theme.LoongPDFTheme
import com.example.ui.viewmodel.DocumentViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoongPDFTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoongPDFApp()
                }
            }
        }
    }
}

@Composable
fun LoongPDFApp() {
    val navController = rememberNavController()
    val viewModel: DocumentViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "document_list"
    ) {
        composable("document_list") {
            DocumentListScreen(
                viewModel = viewModel,
                onOpenEditor = {
                    navController.navigate("editor")
                }
            )
        }

        composable("editor") {
            EditorAndPreviewScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

package com.notianotes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.notianotes.app.ui.TestInkScreen
import com.notianotes.app.ui.theme.NotiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotiaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Just a container, pass modifier padding via a Box if needed, 
                    // but TestInkScreen handles fillMaxSize.
                    // For simplicity, we just show TestInkScreen, ignoring padding to use full screen for drawing.
                    TestInkScreen()
                }
            }
        }
    }
}
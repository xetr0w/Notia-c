package com.notianotes.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.notianotes.app.ink.DrawingSurface
import com.notianotes.app.ink.FreehandStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestInkScreen() {
    val context = LocalContext.current
    
    // New State for Custom Engine
    val strokes = remember { mutableStateListOf<FreehandStroke>() }

    // State for specific brush properties
    var selectedBrushType by remember { mutableStateOf("Fountain") }
    var brushSize by remember { mutableFloatStateOf(4f) }
    var brushColor by remember { mutableStateOf(Color.Black) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                // Row 1: Brush Selection
                BrushTypeSelector(
                    selectedType = selectedBrushType,
                    onTypeSelected = { type -> 
                        selectedBrushType = type 
                        // Update default sizes for specific types
                        if (type == "Highlighter" || type == "Marker") {
                            if (brushSize < 10f) brushSize = 25f
                        } else if (type == "Pencil") {
                            brushSize = 5f
                        } else {
                            if (brushSize > 10f) brushSize = 4f
                        }
                    }
                )
                
                // Row 2: Settings (Size + Color)
                BrushSettingsBar(
                    size = brushSize,
                    onSizeChange = { brushSize = it },
                    color = brushColor,
                    onColorChange = { brushColor = it }
                )
                Divider()
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            DrawingSurface(
                strokes = strokes,
                currentBrushType = selectedBrushType,
                currentBrushSize = brushSize,
                currentBrushColor = brushColor,
                backgroundImageUri = null,
                modifier = Modifier.fillMaxSize()
            )
            
            // Action Buttons
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Clear Button
                FloatingActionButton(
                    onClick = { strokes.clear() },
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text("Clear")
                }
                
                // Undo Button
                FloatingActionButton(
                    onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text("Undo")
                }
            }
        }
    }
}

@Composable
fun BrushTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val types = listOf(
            "Fountain" to "Dolma",
            "Ballpoint" to "Tükenmez",
            "Highlighter" to "Fosforlu",
            "Marker" to "Yuvarlak",
            "Calligraphy" to "Kaligrafi",
            "Pencil" to "Kurşun"
        )
        
        types.forEach { (id, label) ->
            FilterChip(
                selected = selectedType == id,
                onClick = { onTypeSelected(id) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun BrushSettingsBar(
    size: Float,
    onSizeChange: (Float) -> Unit,
    color: Color,
    onColorChange: (Color) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color Button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
                .clickable { 
                    // Cycle simple colors for demo
                    val newColor = when(color) {
                        Color.Black -> Color.Blue
                        Color.Blue -> Color.Red
                        Color.Red -> Color.Green
                        Color.Green -> Color.Yellow
                        else -> Color.Black
                    }
                    onColorChange(newColor)
                }
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text("Size: ${size.toInt()}", style = MaterialTheme.typography.bodySmall)
        
        Slider(
            value = size,
            onValueChange = onSizeChange,
            valueRange = 1f..50f,
            modifier = Modifier.weight(1f)
        )
    }
}

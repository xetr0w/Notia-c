package com.notianotes.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.ink.brush.Brush
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import com.notianotes.app.ink.CustomBrushes
import com.notianotes.app.ink.DrawingSurface
import com.notianotes.app.ink.NotiaBrushes
import com.notianotes.app.ink.NotiaTextureBitmapStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestInkScreen() {
    val context = LocalContext.current
    val strokes = remember { mutableStateListOf<Stroke>() }
    val textureStore = remember { NotiaTextureBitmapStore(context) }
    val customBrushes = remember { CustomBrushes.getBrushes(context) }
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create(textureStore = textureStore) }

    // State for specific brush properties
    var selectedBrushType by remember { mutableStateOf("Fountain") }
    var brushSize by remember { mutableFloatStateOf(4f) }
    var brushColor by remember { mutableStateOf(Color.Black) }

    // Derive currentBrush from state
    val currentBrush = remember(selectedBrushType, brushSize, brushColor) {
        when (selectedBrushType) {
            "Fountain" -> NotiaBrushes.fountainPen(brushColor, brushSize)
            "Ballpoint" -> NotiaBrushes.ballpointPen(brushColor, brushSize)
            "Highlighter" -> NotiaBrushes.highlighter(brushColor, brushSize) // Note: Highlighter defaults to large size, maybe scale?
            "Marker" -> NotiaBrushes.roundMarker(brushColor, brushSize)
            "Calligraphy" -> NotiaBrushes.calligraphy(customBrushes, brushColor, brushSize)
            "Pencil" -> NotiaBrushes.pencil(brushColor, brushSize)
            else -> NotiaBrushes.fountainPen(brushColor, brushSize)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                // Row 1: Brush Selection
                BrushTypeSelector(
                    selectedType = selectedBrushType,
                    onTypeSelected = { type -> 
                        selectedBrushType = type 
                        // Update default sizes for specific types if needed
                        if (type == "Highlighter" || type == "Marker") {
                            if (brushSize < 10f) brushSize = 18f
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
                canvasStrokeRenderer = canvasStrokeRenderer,
                textureStore = textureStore,
                onStrokesFinished = { newStrokes -> strokes.addAll(newStrokes) },
                onErase = { _, _ -> },
                onEraseStart = {},
                onEraseEnd = {},
                currentBrush = currentBrush,
                onGetNextBrush = { currentBrush },
                isEraserMode = false,
                backgroundImageUri = null,
                onStartDrag = {},
                modifier = Modifier.fillMaxSize()
            )
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
        
        Text("Boyut: ${size.toInt()}", style = MaterialTheme.typography.bodySmall)
        
        Slider(
            value = size,
            onValueChange = onSizeChange,
            valueRange = 1f..30f,
            modifier = Modifier.weight(1f)
        )
    }
}

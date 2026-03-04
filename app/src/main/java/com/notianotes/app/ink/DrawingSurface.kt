package com.notianotes.app.ink

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.notianotes.app.freehand.FreehandAlgorithm
import com.notianotes.app.freehand.StrokePoint
import com.notianotes.app.render.FreehandRenderer

/**
 * Completed stroke with cached path
 */
data class FreehandStroke(
    val points: List<StrokePoint>,
    val color: Color,
    val size: Float,
    val type: String,
    val cachedPath: Path
)

@SuppressLint("RestrictedApi")
@Composable
fun DrawingSurface(
    strokes: MutableList<FreehandStroke>,
    currentBrushType: String,
    currentBrushSize: Float,
    currentBrushColor: Color,
    backgroundImageUri: String?,
    modifier: Modifier = Modifier
) {
    // Transform state
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    // Drawing state - use SnapshotStateList for proper recomposition
    val currentPoints = remember { mutableStateListOf<StrokePoint>() }
    var isDrawing by remember { mutableStateOf(false) }
    
    // Precompute display color
    val displayColor = remember(currentBrushType, currentBrushColor) {
        getDisplayColor(currentBrushType, currentBrushColor)
    }
    
    // Precompute thinning factor
    val thinning = remember(currentBrushType) {
        getThinningFactor(currentBrushType)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = pan.x
                    translationY = pan.y
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                }
                // Zoom/Pan handler
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val changes = event.changes
                            
                            if (changes.any { it.type == PointerType.Stylus }) continue
                            
                            if (changes.all { it.type == PointerType.Touch } && changes.size >= 2) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = false)
                                
                                if (zoomChange != 1f || panChange != Offset.Zero) {
                                    val newZoom = (zoom * zoomChange).coerceIn(1f, 10f)
                                    val zoomFactor = newZoom / zoom
                                    
                                    pan = Offset(
                                        (pan.x - centroid.x) * zoomFactor + centroid.x + panChange.x,
                                        (pan.y - centroid.y) * zoomFactor + centroid.y + panChange.y
                                    )
                                    zoom = newZoom
                                    changes.forEach { it.consume() }
                                }
                            }
                        } while (changes.any { it.pressed })
                    }
                }
                // Drawing handler
                .pointerInput(currentBrushType, currentBrushSize, currentBrushColor) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        
                        if (down.type == PointerType.Stylus) {
                            // Start drawing
                            isDrawing = true
                            currentPoints.clear()
                            val startPoint = StrokePoint(
                                x = down.position.x,
                                y = down.position.y,
                                pressure = down.pressure.coerceIn(0.1f, 1f),
                                time = System.currentTimeMillis()
                            )
                            currentPoints.add(startPoint)
                            down.consume()
                            
                            // Son bilinen pozisyonu takip et
                            var lastPosition = down.position
                            var lastPressure = down.pressure
                            
                            // Continue drawing
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                
                                if (change == null || !change.pressed) {
                                    // DÜZELTME 1: Kalem kaldırıldı!
                                    // Eğer son kaydedilen nokta ile kalemin kalktığı nokta arasında fark varsa,
                                    // o son minik hareketi de ekle. Yoksa stroke ucu eksik kalır.
                                    val lastRecorded = currentPoints.lastOrNull()
                                    if (lastRecorded != null) {
                                        val dist = (lastRecorded.x - lastPosition.x) * (lastRecorded.x - lastPosition.x) + 
                                                   (lastRecorded.y - lastPosition.y) * (lastRecorded.y - lastPosition.y)
                                        // Çok çok küçük değilse ekle (0.1f bile olsa ekle ki yön belli olsun)
                                        if (dist > 0.1f) {
                                            currentPoints.add(StrokePoint(
                                                x = lastPosition.x,
                                                y = lastPosition.y,
                                                pressure = lastPressure.coerceIn(0.1f, 1f),
                                                time = System.currentTimeMillis()
                                            ))
                                        }
                                    }
                                    break
                                }
                                
                                // Pozisyonu güncelle
                                lastPosition = change.position
                                lastPressure = change.pressure

                                // Add point if moved enough
                                val last = currentPoints.lastOrNull()
                                val dx = change.position.x - (last?.x ?: 0f)
                                val dy = change.position.y - (last?.y ?: 0f)
                                
                                // DÜZELTME 2: Hassasiyeti artırdım (4f -> 1f).
                                // Daha sık nokta alırsak polygonal görüntü azalır.
                                if (last == null || (dx * dx + dy * dy) > 1f) { 
                                    currentPoints.add(StrokePoint(
                                        x = change.position.x,
                                        y = change.position.y,
                                        pressure = change.pressure.coerceIn(0.1f, 1f),
                                        time = System.currentTimeMillis()
                                    ))
                                }
                                change.consume()
                            }
                            
                            // End drawing - commit stroke
                            isDrawing = false
                            
                            if (currentPoints.isNotEmpty()) {
                                val points = currentPoints.toList()
                                val outline = FreehandAlgorithm.getStrokeOutline(
                                    points = points,
                                    baseSize = currentBrushSize,
                                    thinning = thinning
                                )
                                
                                if (outline.isNotEmpty()) {
                                    val path = FreehandRenderer.createSmoothPath(outline)
                                    strokes.add(FreehandStroke(
                                        points = points,
                                        color = displayColor,
                                        size = currentBrushSize,
                                        type = currentBrushType,
                                        cachedPath = path
                                    ))
                                }
                            }
                            currentPoints.clear()
                        }
                    }
                }
        ) {
            // Background image
            backgroundImageUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Canvas for drawing strokes
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw completed strokes
                strokes.forEach { stroke ->
                    drawPath(stroke.cachedPath, stroke.color)
                }
                
                // Draw current stroke (wet/live)
                val points = currentPoints.toList()
                if (points.isNotEmpty()) {
                    val outline = FreehandAlgorithm.getStrokeOutline(
                        points = points,
                        baseSize = currentBrushSize,
                        thinning = thinning
                    )
                    
                    if (outline.isNotEmpty()) {
                        val livePath = FreehandRenderer.createSmoothPath(outline)
                        drawPath(livePath, displayColor)
                    }
                }
            }
        }
    }
}

// Helper functions (Aynı kalabilir)
fun getThinningFactor(type: String): Float {
    return when (type) {
        "Fountain" -> 0.5f
        "Calligraphy" -> 0.6f
        "Pencil" -> 0.3f
        "Ballpoint" -> 0f      
        "Marker" -> 0f
        "Highlighter" -> 0f
        else -> 0.4f
    }
}

fun getDisplayColor(type: String, baseColor: Color): Color {
    return when (type) {
        "Highlighter" -> baseColor.copy(alpha = 0.35f)
        "Marker" -> baseColor.copy(alpha = 0.6f)
        "Pencil" -> baseColor.copy(alpha = 0.8f)
        else -> baseColor
    }
}
package com.notianotes.app.ink

import android.annotation.SuppressLint
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.withSave
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.TextureBitmapStore
import androidx.ink.brush.compose.createWithComposeColor
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import coil3.compose.AsyncImage
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.ui.input.pointer.positionChanged


@SuppressLint("RestrictedApi", "VisibleForTests")
@Composable
fun DrawingSurface(
    strokes: List<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer,
    textureStore: TextureBitmapStore? = null,
    onStrokesFinished: (List<Stroke>) -> Unit,
    onErase: (offsetX: Float, offsetY: Float) -> Unit,
    onEraseStart: () -> Unit,
    onEraseEnd: () -> Unit,
    currentBrush: Brush,
    onGetNextBrush: () -> Brush,
    isEraserMode: Boolean,
    backgroundImageUri: String?,
    onStartDrag: () -> Unit,
    modifier: Modifier = Modifier
) {
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    // Gesture Logic for Zoom/Pan (Input Separation)
    // We apply this to the Content Wrapper.
    // It captures FINGERS in the INITIAL pass to:
    // 1. Perform Zoom/Pan (since it sees them first)
    // 2. CONSUME them (so InProgressStrokes doesn't see them)
    // It ignores STYLUS (letting InProgressStrokes see them)
    
    val zoomModifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            var rotation = 0f
            var zoomFactor = 1f
            var panFactor = androidx.compose.ui.geometry.Offset.Zero
            var pastTouchSlop = false
            val touchSlop = viewConfiguration.touchSlop

            awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
            
            do {
                val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                
                // FILTER: Only process if ALL pointers are TOUCH (Finger)
                val isFinger = event.changes.all { it.type == androidx.compose.ui.input.pointer.PointerType.Touch }
                val isStylus = event.changes.any { it.type == androidx.compose.ui.input.pointer.PointerType.Stylus }

                if (isStylus) {
                     // If stylus, do NOTHING. Do not consume. 
                     // Let it propagate to InProgressStrokes (Child).
                     // We just continue monitoring.
                     continue
                }
                
                // If Finger, we handle Zoom/Pan and CONSUME.
                val canceled = event.changes.any { it.isConsumed }
                if (!canceled && isFinger) {
                    val zoomChange = event.calculateZoom()
                    val panChange = event.calculatePan()
                    
                    if (!pastTouchSlop) {
                        zoomFactor *= zoomChange
                        panFactor += panChange
                        
                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                        val zoomMotion = kotlin.math.abs(1 - zoomFactor) * centroidSize
                        val panMotion = panFactor.getDistance()
                        
                        if (zoomMotion > touchSlop || panMotion > touchSlop) {
                            pastTouchSlop = true
                        }
                    }
                    
                    if (pastTouchSlop) {
                         val centroid = event.calculateCentroid(useCurrent = false)
                         if (zoomChange != 1f || panChange != androidx.compose.ui.geometry.Offset.Zero) {
                             // Apply to State
                             val oldZoom = zoom
                             val newZoom = (zoom * zoomChange).coerceIn(1f, 10f)
                             val effectiveZoomChange = newZoom / oldZoom
                             
                             val x = (pan.x - centroid.x) * effectiveZoomChange + centroid.x
                             val y = (pan.y - centroid.y) * effectiveZoomChange + centroid.y
                             
                             zoom = newZoom
                             pan = androidx.compose.ui.geometry.Offset(x + panChange.x, y + panChange.y)
                         }
                         
                         event.changes.forEach {
                             if (it.positionChanged()) {
                                 it.consume()
                             }
                         }
                    }
                }
            } while (!event.changes.all { !it.pressed } && !event.changes.any { it.isConsumed })
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Transformable Content Wrapper
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
                .then(zoomModifier)
        ) {
            backgroundImageUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Background Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvas = drawContext.canvas.nativeCanvas
                strokes.forEach { stroke ->
                    val blendMode = if (stroke.brush.family == StockBrushes.highlighter()) {
                        BlendMode.Multiply
                    } else {
                        BlendMode.SrcOver
                    }
                    
                   val paint = androidx.compose.ui.graphics.Paint().apply { this.blendMode = blendMode }

                    drawContext.canvas.withSaveLayer(drawContext.size.toRect(), paint) {
                         canvas.withSave {
                             canvasStrokeRenderer.draw(
                                 stroke = stroke,
                                 canvas = this,
                                 strokeToScreenTransform = Matrix() 
                             )
                         }
                    }
                }
            }
            
            // InProgressStrokes
            // Removed key(currentBrush) to allow internal updates without killing the component
            textureStore?.let {
                InProgressStrokes(
                    defaultBrush = currentBrush,
                    nextBrush = onGetNextBrush,
                    onStrokesFinished = onStrokesFinished,
                    textureBitmapStore = it
                )
            } ?: InProgressStrokes(
                defaultBrush = currentBrush,
                nextBrush = onGetNextBrush,
                onStrokesFinished = onStrokesFinished,
            )
            
            // Eraser Logic
             if (isEraserMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { onEraseStart() },
                                onDragEnd = { onEraseEnd() }
                            ) { change, _ ->
                                onErase(change.position.x, change.position.y)
                                change.consume()
                            }
                        }
                )
             }
        }
    }
}

@Preview
@Composable
fun DrawingSurfacePreview() {
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }
    var currentBrush by remember {
        mutableStateOf(
            Brush.createWithComposeColor(
                family = StockBrushes.highlighter(),
                color = androidx.compose.ui.graphics.Color.Blue,
                size = 10F,
                epsilon = 0.01F
            )
        )
    }

    DrawingSurface(
        strokes = emptyList(),
        canvasStrokeRenderer = canvasStrokeRenderer,
        onStrokesFinished = {},
        onErase = { _, _ -> },
        onEraseStart = {},
        onEraseEnd = {},
        currentBrush = currentBrush,
        onGetNextBrush = { currentBrush },
        isEraserMode = false,
        backgroundImageUri = null,
        onStartDrag = {}
    )
}

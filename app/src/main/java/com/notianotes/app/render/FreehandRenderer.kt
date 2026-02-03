package com.notianotes.app.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.notianotes.app.freehand.Point

/**
 * Simplified Freehand Renderer
 * Creates smooth paths from outline points using quadratic bezier curves
 */
object FreehandRenderer {

    /**
     * Draw outline as a smooth filled path
     */
    fun drawOutline(
        drawScope: DrawScope,
        outline: List<Point>,
        color: Color,
        isEraser: Boolean = false
    ) {
        if (outline.size < 3) return
        
        val path = createSmoothPath(outline)
        drawScope.drawPath(
            path = path,
            color = if (isEraser) Color.White else color
        )
    }

    /**
     * Create a smooth closed path from outline points
     * Uses quadratic bezier curves for smooth edges
     */
    fun createSmoothPath(outline: List<Point>): Path {
        if (outline.isEmpty()) return Path()
        
        if (outline.size < 3) {
            // Too few points, draw simple polygon
            return Path().apply {
                moveTo(outline.first().x, outline.first().y)
                outline.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
        }
        
        val path = Path()
        
        // Start at midpoint between last and first point
        val firstMid = midpoint(outline.last(), outline.first())
        path.moveTo(firstMid.x, firstMid.y)
        
        // Draw smooth curves through all points
        for (i in outline.indices) {
            val current = outline[i]
            val next = outline[(i + 1) % outline.size]
            val mid = midpoint(current, next)
            
            // Quadratic bezier: current point is control point, midpoint is end
            path.quadraticBezierTo(current.x, current.y, mid.x, mid.y)
        }
        
        path.close()
        return path
    }
    
    /**
     * Create path for caching (same as createSmoothPath)
     */
    fun createCachedPath(outline: List<Point>): Path {
        return createSmoothPath(outline)
    }
    
    private fun midpoint(a: Point, b: Point): Point {
        return Point((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    }
}
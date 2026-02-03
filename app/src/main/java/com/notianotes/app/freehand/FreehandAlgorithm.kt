package com.notianotes.app.freehand

import kotlin.math.sqrt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.abs

/**
 * Simplified Freehand Algorithm - V5
 * Fixed arc direction for proper rounded caps (no more crescent shapes)
 */
object FreehandAlgorithm {

    /**
     * Generate stroke outline - SINGLE algorithm for both wet and dry
     */
    fun getStrokeOutline(
        points: List<StrokePoint>,
        baseSize: Float,
        thinning: Float = 0.5f,
        taperStart: Float = 0f,
        taperEnd: Float = 0f
    ): List<Point> {
        if (points.isEmpty()) return emptyList()
        
        // Single point = circle
        if (points.size == 1) {
            return createCircle(points.first().toPoint(), baseSize / 2f)
        }
        
        // Very short stroke = circle at center
        val totalDist = totalDistance(points)
        if (totalDist < baseSize * 0.5f) {
            val center = centerOfMass(points)
            return createCircle(center, baseSize / 2f)
        }
        
        // Two points = capsule
        if (points.size == 2) {
            return createCapsule(
                points[0].toPoint(),
                points[1].toPoint(),
                getWidth(points[0], baseSize, thinning) / 2f,
                getWidth(points[1], baseSize, thinning) / 2f
            )
        }
        
        // Normal stroke: generate left and right edges
        val leftEdge = mutableListOf<Point>()
        val rightEdge = mutableListOf<Point>()
        
        for (i in points.indices) {
            val p = points[i]
            val width = getWidth(p, baseSize, thinning)
            val normal = getNormal(points, i)
            val halfWidth = width / 2f
            
            leftEdge.add(p.toPoint() + normal * halfWidth)
            rightEdge.add(p.toPoint() - normal * halfWidth)
        }
        
        // Build closed outline with SIMPLE rounded caps
        val outline = mutableListOf<Point>()
        
        // Start cap - semicircle connecting right to left
        val startCenter = points.first().toPoint()
        val startRadius = getWidth(points.first(), baseSize, thinning) / 2f
        outline.addAll(createSemicircle(startCenter, startRadius, rightEdge.first(), leftEdge.first()))
        
        // Left edge (top)
        outline.addAll(leftEdge)
        
        // End cap - semicircle connecting left to right
        val endCenter = points.last().toPoint()
        val endRadius = getWidth(points.last(), baseSize, thinning) / 2f
        outline.addAll(createSemicircle(endCenter, endRadius, leftEdge.last(), rightEdge.last()))
        
        // Right edge reversed (bottom)
        outline.addAll(rightEdge.reversed())
        
        return outline
    }
    
    /**
     * Fast version - same as regular (for API compatibility)
     */
    fun getStrokeOutlineFast(
        points: List<StrokePoint>,
        baseSize: Float,
        thinning: Float = 0.5f
    ): List<Point> {
        return getStrokeOutline(points, baseSize, thinning, 0f, 0f)
    }
    
    // ===== HELPER FUNCTIONS =====
    
    private fun getWidth(point: StrokePoint, baseSize: Float, thinning: Float): Float {
        if (thinning <= 0f) return baseSize
        
        val pressure = point.pressure.coerceIn(0.2f, 1f)
        val minWidth = baseSize * 0.4f
        val maxWidth = baseSize
        return minWidth + (maxWidth - minWidth) * pressure
    }
    
    private fun getNormal(points: List<StrokePoint>, index: Int): Point {
        // Liste çok kısaysa varsayılan döndür
        if (points.size < 2) return Point(0f, 1f)

        // Güvenli prev/next seçimi
        val prevIndex = (index - 1).coerceAtLeast(0)
        val nextIndex = (index + 1).coerceAtMost(points.lastIndex)

        var prev = points[prevIndex].toPoint()
        var next = points[nextIndex].toPoint()

        // Eğer prev ve next aynıysa (başlangıç/bitiş noktalarında olabilir),
        // biraz daha uzağa bakmaya çalışalım
        if (prev.distanceTo(next) < 0.001f) {
            if (nextIndex < points.lastIndex) {
                next = points[nextIndex + 1].toPoint()
            } else if (prevIndex > 0) {
                prev = points[prevIndex - 1].toPoint()
            }
        }

        val dx = next.x - prev.x
        val dy = next.y - prev.y
        val len = sqrt(dx * dx + dy * dy)
        
        // Hala 0 ise (tek noktaya çoklu basım), varsayılan dik vektör
        return if (len < 0.001f) {
            Point(0f, 1f)
        } else {
            Point(-dy / len, dx / len)
        }
    }
    
    private fun totalDistance(points: List<StrokePoint>): Float {
        var dist = 0f
        for (i in 1 until points.size) {
            dist += points[i].distanceTo(points[i - 1])
        }
        return dist
    }
    
    private fun centerOfMass(points: List<StrokePoint>): Point {
        val x = points.map { it.x }.average().toFloat()
        val y = points.map { it.y }.average().toFloat()
        return Point(x, y)
    }
    
    /**
     * Create a full circle
     */
    private fun createCircle(center: Point, radius: Float): List<Point> {
        val r = radius.coerceAtLeast(2f)
        val segments = 16
        return (0 until segments).map { i ->
            val angle = (2.0 * PI * i / segments).toFloat()
            Point(
                center.x + r * cos(angle),
                center.y + r * sin(angle)
            )
        }
    }
    
    /**
     * Create a semicircle from 'from' point to 'to' point around 'center'
     * Always creates a proper half-circle arc for rounded caps
     */
    private fun createSemicircle(
        center: Point,
        radius: Float,
        from: Point,
        to: Point
    ): List<Point> {
        val r = radius.coerceAtLeast(1f)
        
        // Calculate angles from center to each point
        val angle1 = atan2(from.y - center.y, from.x - center.x)
        val angle2 = atan2(to.y - center.y, to.x - center.x)
        
        // Calculate angle difference
        var angleDiff = angle2 - angle1
        
        // Normalize to [-PI, PI]
        while (angleDiff > PI) angleDiff -= (2 * PI).toFloat()
        while (angleDiff < -PI) angleDiff += (2 * PI).toFloat()
        
        // For a cap, we want the arc that goes "outside" (the longer way around)
        // If angleDiff is positive but less than PI, go the other way (add 2*PI)
        // If angleDiff is negative but greater than -PI, go the other way (subtract 2*PI)
        if (abs(angleDiff) < PI) {
            // We're going the short way - need to go the long way instead
            if (angleDiff > 0) {
                angleDiff -= (2 * PI).toFloat()
            } else {
                angleDiff += (2 * PI).toFloat()
            }
        }
        
        // Generate the arc
        val segments = 8
        val result = mutableListOf<Point>()
        
        for (i in 0..segments) {
            val t = i.toFloat() / segments
            val angle = angle1 + angleDiff * t
            result.add(Point(
                center.x + r * cos(angle),
                center.y + r * sin(angle)
            ))
        }
        
        return result
    }
    
    /**
     * Create a capsule (pill shape) between two points
     */
    private fun createCapsule(p1: Point, p2: Point, r1: Float, r2: Float): List<Point> {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val len = sqrt(dx * dx + dy * dy)
        
        if (len < 1f) {
            return createCircle(Point((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f), (r1 + r2) / 2f)
        }
        
        // Direction and normal
        val dirX = dx / len
        val dirY = dy / len
        val normX = -dirY
        val normY = dirX
        
        val radius1 = r1.coerceAtLeast(2f)
        val radius2 = r2.coerceAtLeast(2f)
        
        // Four corners
        val p1Left = Point(p1.x + normX * radius1, p1.y + normY * radius1)
        val p1Right = Point(p1.x - normX * radius1, p1.y - normY * radius1)
        val p2Left = Point(p2.x + normX * radius2, p2.y + normY * radius2)
        val p2Right = Point(p2.x - normX * radius2, p2.y - normY * radius2)
        
        val result = mutableListOf<Point>()
        
        // Start cap
        result.addAll(createSemicircle(p1, radius1, p1Right, p1Left))
        
        // Top edge to p2
        result.add(p2Left)
        
        // End cap
        result.addAll(createSemicircle(p2, radius2, p2Left, p2Right))
        
        // Bottom edge back to start
        result.add(p1Right)
        
        return result
    }
}
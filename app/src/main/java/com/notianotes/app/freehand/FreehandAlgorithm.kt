package com.notianotes.app.freehand

import kotlin.math.sqrt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.abs

/**
 * Simplified Freehand Algorithm - V6 (Smoothed)
 * Includes Catmull-Rom spline interpolation for smooth spines without rendering artifacts
 */
object FreehandAlgorithm {

    /**
     * Generate stroke outline - Now with input smoothing!
     */
    fun getStrokeOutline(
        points: List<StrokePoint>,
        baseSize: Float,
        thinning: Float = 0.5f,
        taperStart: Float = 0f,
        taperEnd: Float = 0f
    ): List<Point> {
        if (points.isEmpty()) return emptyList()

        // ADIM 1: Girdiyi yumuşat (Catmull-Rom Spline)
        // Bu işlem "polygonal" görünümü engeller.
        val smoothedPoints = smoothStroke(points)

        // Single point = circle
        if (smoothedPoints.size == 1) {
            return createCircle(smoothedPoints.first().toPoint(), baseSize / 2f)
        }

        // Very short stroke = circle at center
        val totalDist = totalDistance(smoothedPoints)
        if (totalDist < baseSize * 0.5f) {
            val center = centerOfMass(smoothedPoints)
            return createCircle(center, baseSize / 2f)
        }

        // Two points = capsule
        if (smoothedPoints.size == 2) {
            return createCapsule(
                smoothedPoints[0].toPoint(),
                smoothedPoints[1].toPoint(),
                getWidth(smoothedPoints[0], baseSize, thinning) / 2f,
                getWidth(smoothedPoints[1], baseSize, thinning) / 2f
            )
        }

        // Normal stroke: generate left and right edges
        val leftEdge = mutableListOf<Point>()
        val rightEdge = mutableListOf<Point>()

        for (i in smoothedPoints.indices) {
            val p = smoothedPoints[i]
            val width = getWidth(p, baseSize, thinning)
            val normal = getNormal(smoothedPoints, i)
            val halfWidth = width / 2f

            leftEdge.add(p.toPoint() + normal * halfWidth)
            rightEdge.add(p.toPoint() - normal * halfWidth)
        }

        // Build closed outline with SIMPLE rounded caps
        val outline = mutableListOf<Point>()

        // Start cap
        val startCenter = smoothedPoints.first().toPoint()
        val startRadius = getWidth(smoothedPoints.first(), baseSize, thinning) / 2f
        outline.addAll(createSemicircle(startCenter, startRadius, rightEdge.first(), leftEdge.first()))

        // Left edge (top)
        outline.addAll(leftEdge)

        // End cap
        val endCenter = smoothedPoints.last().toPoint()
        val endRadius = getWidth(smoothedPoints.last(), baseSize, thinning) / 2f
        outline.addAll(createSemicircle(endCenter, endRadius, leftEdge.last(), rightEdge.last()))

        // Right edge reversed (bottom)
        outline.addAll(rightEdge.reversed())

        return outline
    }

    fun getStrokeOutlineFast(
        points: List<StrokePoint>,
        baseSize: Float,
        thinning: Float = 0.5f
    ): List<Point> {
        return getStrokeOutline(points, baseSize, thinning, 0f, 0f)
    }

    // ===== SMOOTHING LOGIC (CATMULL-ROM) =====

    private fun smoothStroke(points: List<StrokePoint>): List<StrokePoint> {
        if (points.size < 3) return points

        val result = mutableListOf<StrokePoint>()

        // Her zaman ilk noktayı ekle
        result.add(points.first())

        for (i in 0 until points.size - 1) {
            val p0 = points[(i - 1).coerceAtLeast(0)]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[(i + 2).coerceAtMost(points.lastIndex)]

            val distance = p1.toPoint().distanceTo(p2.toPoint())
            
            // Eğer iki nokta arası çok açıksa araya nokta serpiştir (Interpolation)
            // Bu değer (4f) ne kadar düşükse çizgi o kadar pürüzsüz ama işlem o kadar artar.
            val segments = (distance / 4f).toInt().coerceAtLeast(1)

            for (j in 1..segments) {
                val t = j.toFloat() / (segments + 1)
                
                // Catmull-Rom formülü ile yeni x, y, pressure hesapla
                val newX = catmullRom(p0.x, p1.x, p2.x, p3.x, t)
                val newY = catmullRom(p0.y, p1.y, p2.y, p3.y, t)
                val newP = lerp(p1.pressure, p2.pressure, t)
                
                // Zamanı (t) basitçe lineer enterpolasyon yapıyoruz
                val newTime = (p1.time + (p2.time - p1.time) * t).toLong()

                result.add(StrokePoint(newX, newY, newP, newTime))
            }
            
            // Orijinal p2'yi bir sonraki döngüde p1 olarak işleyeceği için burada eklemiyoruz,
            // ama son segment ise eklememiz lazım.
             result.add(p2)
        }

        return result
    }

    private fun catmullRom(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val t2 = t * t
        val t3 = t2 * t
        return 0.5f * (
                (2f * p1) +
                (-p0 + p2) * t +
                (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
                (-p0 + 3f * p1 - 3f * p2 + p3) * t3
        )
    }

    private fun lerp(start: Float, stop: Float, amount: Float): Float {
        return start + (stop - start) * amount
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
        val prev = if (index > 0) points[index - 1].toPoint() else points[index].toPoint()
        val next = if (index < points.lastIndex) points[index + 1].toPoint() else points[index].toPoint()

        val dx = next.x - prev.x
        val dy = next.y - prev.y
        val len = sqrt(dx * dx + dy * dy)

        return if (len < 0.001f) Point(0f, 1f) else Point(-dy / len, dx / len)
    }

    private fun totalDistance(points: List<StrokePoint>): Float {
        var dist = 0f
        for (i in 1 until points.size) dist += points[i].distanceTo(points[i - 1])
        return dist
    }

    private fun centerOfMass(points: List<StrokePoint>): Point {
        val x = points.map { it.x }.average().toFloat()
        val y = points.map { it.y }.average().toFloat()
        return Point(x, y)
    }

    // Segments sayısını artırdım (16 -> 32)
    private fun createCircle(center: Point, radius: Float): List<Point> {
        val r = radius.coerceAtLeast(2f)
        val segments = 32 
        return (0 until segments).map { i ->
            val angle = (2.0 * PI * i / segments).toFloat()
            Point(center.x + r * cos(angle), center.y + r * sin(angle))
        }
    }

    // Segments sayısını artırdım (8 -> 16)
    private fun createSemicircle(center: Point, radius: Float, from: Point, to: Point): List<Point> {
        val r = radius.coerceAtLeast(1f)
        val angle1 = atan2(from.y - center.y, from.x - center.x)
        val angle2 = atan2(to.y - center.y, to.x - center.x)
        var angleDiff = angle2 - angle1
        while (angleDiff > PI) angleDiff -= (2 * PI).toFloat()
        while (angleDiff < -PI) angleDiff += (2 * PI).toFloat()
        
        if (abs(angleDiff) < PI) {
            if (angleDiff > 0) angleDiff -= (2 * PI).toFloat() else angleDiff += (2 * PI).toFloat()
        }

        val segments = 16 
        val result = mutableListOf<Point>()
        for (i in 0..segments) {
            val t = i.toFloat() / segments
            val angle = angle1 + angleDiff * t
            result.add(Point(center.x + r * cos(angle), center.y + r * sin(angle)))
        }
        return result
    }
    
    private fun createCapsule(p1: Point, p2: Point, r1: Float, r2: Float): List<Point> {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val len = sqrt(dx * dx + dy * dy)
        if (len < 1f) return createCircle(Point((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f), (r1 + r2) / 2f)

        val dirX = dx / len
        val dirY = dy / len
        val normX = -dirY
        val normY = dirX
        val radius1 = r1.coerceAtLeast(2f)
        val radius2 = r2.coerceAtLeast(2f)

        val p1Left = Point(p1.x + normX * radius1, p1.y + normY * radius1)
        val p1Right = Point(p1.x - normX * radius1, p1.y - normY * radius1)
        val p2Left = Point(p2.x + normX * radius2, p2.y + normY * radius2)
        val p2Right = Point(p2.x - normX * radius2, p2.y - normY * radius2)

        val result = mutableListOf<Point>()
        result.addAll(createSemicircle(p1, radius1, p1Right, p1Left))
        result.add(p2Left)
        result.addAll(createSemicircle(p2, radius2, p2Left, p2Right))
        result.add(p1Right)
        return result
    }
}
package com.notianotes.app.freehand

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

data class Point(val x: Float, val y: Float) {
    fun toOffset() = Offset(x, y)
    
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Point(x * scalar, y * scalar)
    operator fun div(scalar: Float) = Point(x / scalar, y / scalar)
    
    fun distanceTo(other: Point): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
    
    fun length(): Float = sqrt(x * x + y * y)
    
    fun normalize(): Point {
        val len = length()
        return if (len < 0.0001f) Point(0f, 0f) else Point(x / len, y / len)
    }
    
    fun perpendicular(): Point = Point(-y, x)
    
    fun dot(other: Point): Float = x * other.x + y * other.y
    
    fun lerp(other: Point, t: Float): Point {
        return Point(
            x + (other.x - x) * t,
            y + (other.y - y) * t
        )
    }
    
    companion object {
        val ZERO = Point(0f, 0f)
    }
}

data class StrokePoint(
    val x: Float, 
    val y: Float, 
    val pressure: Float = 0.5f,
    val time: Long = System.currentTimeMillis()
) {
    fun toPoint() = Point(x, y)
    
    fun distanceTo(other: StrokePoint): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
    
    fun lerp(other: StrokePoint, t: Float): StrokePoint {
        return StrokePoint(
            x = x + (other.x - x) * t,
            y = y + (other.y - y) * t,
            pressure = pressure + (other.pressure - pressure) * t,
            time = time + ((other.time - time) * t).toLong()
        )
    }
}

/**
 * Stroke options for different brush types
 */
data class StrokeOptions(
    val size: Float = 4f,
    val thinning: Float = 0.5f,      // 0 = no pressure effect, 1 = max pressure effect
    val smoothing: Float = 0.5f,     // EMA smoothing factor
    val streamline: Float = 0.5f,    // Point filtering
    val taperStart: Float = 0f,      // Start taper (0-1)
    val taperEnd: Float = 0f,        // End taper (0-1)
    val simulatePressure: Boolean = true,
    val capStart: Boolean = true,
    val capEnd: Boolean = true
)
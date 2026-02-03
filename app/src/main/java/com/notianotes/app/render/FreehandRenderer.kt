package com.notianotes.app.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.notianotes.app.freehand.Point

/**
 * FreehandRenderer - V2
 * FIX: Artık quadraticBezierTo ile yumuşatma yapılmıyor.
 * FreehandAlgorithm zaten detaylı outline (8-16 segment ile yaylar/daireler) üretiyor.
 * Bezier yumuşatması stroke uçlarını "yiyordu" ve titremye neden oluyordu.
 */
object FreehandRenderer {

    fun drawOutline(
        drawScope: DrawScope,
        outline: List<Point>,
        color: Color,
        isEraser: Boolean = false
    ) {
        if (outline.size < 3) return
        
        val path = createPathFromOutline(outline)
        drawScope.drawPath(
            path = path,
            color = if (isEraser) Color.White else color
        )
    }

    /**
     * Outline noktalarını doğrudan lineTo ile birleştir.
     * Algoritma zaten daireler için 16, yaylar için 8 nokta koyduğu için
     * bu çizgiler göze "kare" gelmeyecek, gayet yuvarlak görünecektir.
     */
    fun createSmoothPath(outline: List<Point>): Path {
        return createPathFromOutline(outline)
    }
    
    private fun createPathFromOutline(outline: List<Point>): Path {
        if (outline.isEmpty()) return Path()
        
        val path = Path()
        
        // İlk noktaya git
        val start = outline.first()
        path.moveTo(start.x, start.y)
        
        // Diğer tüm noktaları düz çizgilerle birleştir
        for (i in 1 until outline.size) {
            val p = outline[i]
            path.lineTo(p.x, p.y)
        }
        
        path.close()
        return path
    }
    
    fun createCachedPath(outline: List<Point>): Path {
        return createPathFromOutline(outline)
    }
}
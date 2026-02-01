package com.notianotes.app.ink

import androidx.compose.ui.graphics.Color
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.createWithComposeColor

object NotiaBrushes {
    
    // 1. Dolma Kalem (Fountain Pen)
    // HAFİF basınç hassasiyeti, zoom için düşük epsilon
    fun fountainPen(color: Color = Color.Black, size: Float = 4f): Brush {
        return Brush.createWithComposeColor(
            family = StockBrushes.pressurePen(),
            color = color,
            size = size, // 3-5f arası ideal
            epsilon = 0.025f // 10x zoom desteği ve yüksek hassasiyet için
        )
    }

    // 2. Tükenmez Kalem (Ballpoint)
    // Sabit kalınlık, marker tabanlı
    fun ballpointPen(color: Color = Color.Blue, size: Float = 2.5f): Brush {
        return Brush.createWithComposeColor(
            family = StockBrushes.marker(),
            color = color,
            size = size, // 2-3f
            epsilon = 0.1f // Standart hassasiyet yeterli
        )
    }

    // 3. Fosforlu Kalem (Highlighter)
    // Düz uçlu, saydam, kalın
    fun highlighter(color: Color = Color.Yellow, size: Float = 18f): Brush {
        return Brush.createWithComposeColor(
            family = StockBrushes.highlighter(),
            color = color, // Genelde alpha zaten highlighter brush family tarafından yönetilir ama biz yine de rengi verebiliriz
            size = size, // 15-20f
            epsilon = 0.1f
        )
    }

    // 4. Yuvarlak Marker (Round Marker)
    // Marker tabanlı ama saydam renk
    fun roundMarker(color: Color = Color.Red, size: Float = 18f): Brush {
        // Rengi saydamlaştır (Alpha 0.4)
        val transparentColor = color.copy(alpha = 0.4f)
        return Brush.createWithComposeColor(
            family = StockBrushes.marker(),
            color = transparentColor,
            size = size, 
            epsilon = 0.1f 
        )
    }

    // 5. Kaligrafi Kalemi
    // CustomBrushes üzerinden yüklenen brush (Varsa)
    // Tilt/eğim desteği brush family'nin kendi özelliğidir
    fun calligraphy(customBrushes: List<CustomBrush>, color: Color = Color.Black, size: Float = 5f): Brush {
        val calligraphyFamily = customBrushes.find { it.name == "Calligraphy" }?.brushFamily 
            ?: StockBrushes.pressurePen() // Fallback
            
        return Brush.createWithComposeColor(
            family = calligraphyFamily,
            color = color,
            size = size,
            epsilon = 0.025f // Şekilli uçlar için daha iyi form koruması
        )
    }

    // 6. Kurşun Kalem (Pencil placeholder)
    // Şimdilik gri pressurePen, ileride texture eklenecek
    fun pencil(color: Color = Color.Gray, size: Float = 3f): Brush {
        return Brush.createWithComposeColor(
            family = StockBrushes.pressurePen(),
            color = color,
            size = size,
            epsilon = 0.05f 
        )
    }
}

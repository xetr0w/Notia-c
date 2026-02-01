package com.notianotes.app.ink

import kotlinx.serialization.Serializable

@Serializable
data class SerializedStroke(
    val inputs: ByteArray,
    val brush: SerializedBrush
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerializedStroke

        if (!inputs.contentEquals(other.inputs)) return false
        if (brush != other.brush) return false

        return true
    }

    override fun hashCode(): Int {
        var result = inputs.contentHashCode()
        result = 31 * result + brush.hashCode()
        return result
    }
}

@Serializable
data class SerializedBrush(
    val size: Float,
    val color: Long,
    val epsilon: Float,
    val stockBrush: SerializedStockBrush,
    val clientBrushFamilyId: String? = null
)

enum class SerializedStockBrush {
    MarkerLatest,
    PressurePenLatest,
    HighlighterLatest,
    DashedLineLatest,
}

package com.notianotes.app.ink

import android.util.Log
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.storage.decode
import androidx.ink.storage.encode
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInputBatch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private const val TAG = "Converters"

class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val stockBrushToEnumValues = mapOf(
            StockBrushes.marker() to SerializedStockBrush.MarkerLatest,
            StockBrushes.pressurePen() to SerializedStockBrush.PressurePenLatest,
            StockBrushes.highlighter() to SerializedStockBrush.HighlighterLatest,
            StockBrushes.dashedLine() to SerializedStockBrush.DashedLineLatest,
        )

        private val enumToStockBrush =
            stockBrushToEnumValues.entries.associate { (key, value) -> value to key }
    }

    private fun serializeBrush(brush: Brush, customBrushes: List<CustomBrush>): SerializedBrush {
        val customBrushName = customBrushes.find { it.brushFamily == brush.family }?.name
        return SerializedBrush(
            size = brush.size,
            color = brush.colorLong,
            epsilon = brush.epsilon,
            stockBrush = stockBrushToEnumValues[brush.family] ?: SerializedStockBrush.MarkerLatest,
            clientBrushFamilyId = customBrushName
        )
    }

    fun serializeStroke(stroke: Stroke, customBrushes: List<CustomBrush>): String {
        val serializedBrush = serializeBrush(stroke.brush, customBrushes)
        val encodedSerializedInputs = ByteArrayOutputStream().use { outputStream ->
            stroke.inputs.encode(outputStream)
            outputStream.toByteArray()
        }

        val serializedStroke = SerializedStroke(
            inputs = encodedSerializedInputs,
            brush = serializedBrush
        )
        return json.encodeToString(serializedStroke)
    }

    private fun deserializeStroke(
        serializedStroke: SerializedStroke,
        customBrushes: List<CustomBrush>
    ): Stroke? {
        val inputs = ByteArrayInputStream(serializedStroke.inputs).use { inputStream ->
            StrokeInputBatch.decode(inputStream)
        }
        val brush = deserializeBrush(serializedStroke.brush, customBrushes)
        return Stroke(brush = brush, inputs = inputs)
    }

    private fun deserializeBrush(
        serializedBrush: SerializedBrush,
        customBrushes: List<CustomBrush>
    ): Brush {
        val stockBrushFamily = enumToStockBrush[serializedBrush.stockBrush]
        val customBrush = customBrushes.find {
            it.name == serializedBrush.clientBrushFamilyId
        }

        val brushFamily = customBrush?.brushFamily ?: stockBrushFamily ?: StockBrushes.marker()

        return Brush.createWithColorLong(
            family = brushFamily,
            colorLong = serializedBrush.color,
            size = serializedBrush.size,
            epsilon = serializedBrush.epsilon,
        )
    }

    fun deserializeStrokeFromString(data: String, customBrushes: List<CustomBrush>): Stroke? {
        val serializedStroke = json.decodeFromString<SerializedStroke>(data)
        return deserializeStroke(serializedStroke, customBrushes)
    }

//    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.let { json.encodeToString(it) }
    }

//    @TypeConverter
    fun toStringList(jsonString: String?): List<String>? {
        if (jsonString == null) {
            return emptyList()
        }
        return try {
            json.decodeFromString<List<String>>(jsonString)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error decoding string list from JSON: $jsonString", e
            )
            emptyList()
        }
    }
}

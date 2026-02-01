package com.notianotes.app.ink

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize

// Typealias replacement for missing PointerInputEventHandler
typealias PointerInputEventHandler = suspend PointerInputScope.() -> Unit

/**
 * A replacement for [androidx.compose.ui.input.pointer] that allows pointer events to fall
 * through to sibling composables instead of just ancestor composables. This just delegates to the
 * normal implementation, but has
 * [androidx.compose.ui.node.PointerInputModifierNode.sharePointerInputWithSiblings] return true.
 */
internal fun Modifier.pointerInputWithSiblingFallthrough(
    pointerInputEventHandler: PointerInputEventHandler
) = this then PointerInputSiblingFallthroughElement(pointerInputEventHandler)

private class PointerInputSiblingFallthroughModifierNode(
    pointerInputEventHandler: PointerInputEventHandler
) : PointerInputModifierNode, DelegatingNode() {

    var pointerInputEventHandler: PointerInputEventHandler
        get() = delegateNode.pointerInputHandler
        set(value) {
            delegateNode.pointerInputHandler = value
        }

    val delegateNode = delegate(
        SuspendingPointerInputModifierNode(pointerInputEventHandler)
    )

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        delegateNode.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        delegateNode.onCancelPointerInput()
    }

    override fun sharePointerInputWithSiblings() = true
}

private data class PointerInputSiblingFallthroughElement(
    val pointerInputEventHandler: PointerInputEventHandler
) : ModifierNodeElement<PointerInputSiblingFallthroughModifierNode>() {

    override fun create() = PointerInputSiblingFallthroughModifierNode(pointerInputEventHandler)

    override fun update(node: PointerInputSiblingFallthroughModifierNode) {
        node.pointerInputEventHandler = pointerInputEventHandler
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "pointerInputWithSiblingFallthrough"
        properties["pointerInputEventHandler"] = pointerInputEventHandler
    }
}

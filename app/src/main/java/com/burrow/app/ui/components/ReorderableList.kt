package com.burrow.app.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.zIndex
import com.burrow.app.viewmodel.DragState
import com.burrow.app.viewmodel.ListKind

/**
 * Long-press-free pointer-drag reordering: tracks each row's on-screen center
 * (mirrors the web version's getBoundingClientRect-based refsMap) and swaps
 * the dragged row with whichever neighbor it has crossed the center of.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    listKind: ListKind,
    dragState: DragState,
    onDragStateChange: (DragState) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, index: Int, isDragging: Boolean, dragHandleModifier: Modifier, rowModifier: Modifier) -> Unit,
) {
    val centers = remember(listKind) { mutableStateMapOf<Int, Float>() }
    Column(modifier) {
        items.forEachIndexed { index, item ->
            val isDragging = dragState.listKey == listKind && dragState.index == index
            val rowModifier = Modifier
                .onGloballyPositioned { coords ->
                    centers[index] = coords.positionInRoot().y + coords.size.height / 2f
                }
                .graphicsLayer {
                    translationY = if (isDragging) dragState.offset else 0f
                }
                .zIndex(if (isDragging) 1f else 0f)

            val dragHandleModifier = Modifier.pointerInput(listKind, item) {
                var currentIndex = index
                var accumOffset = 0f
                while (true) {
                    detectDragGestures(
                        onDragStart = {
                            currentIndex = index
                            accumOffset = 0f
                            onDragStateChange(DragState(listKind, currentIndex, 0f))
                        },
                        onDragEnd = { onDragStateChange(DragState()) },
                        onDragCancel = { onDragStateChange(DragState()) },
                    ) { change, dragAmount ->
                        change.consume()
                        accumOffset += dragAmount.y
                        onDragStateChange(DragState(listKind, currentIndex, accumOffset))

                        val baseCenter = centers[currentIndex]
                        if (baseCenter != null) {
                            val draggedCenter = baseCenter + accumOffset
                            var swapWith = -1
                            centers.forEach { (i, c) ->
                                if (i == currentIndex) return@forEach
                                if (i < currentIndex && draggedCenter < c) swapWith = i
                                if (i > currentIndex && draggedCenter > c) swapWith = i
                            }
                            if (swapWith != -1) {
                                onMove(currentIndex, swapWith)
                                currentIndex = swapWith
                                accumOffset = 0f
                                onDragStateChange(DragState(listKind, currentIndex, 0f))
                            }
                        }
                    }
                }
            }

            itemContent(item, index, isDragging, dragHandleModifier, rowModifier)
        }
    }
}

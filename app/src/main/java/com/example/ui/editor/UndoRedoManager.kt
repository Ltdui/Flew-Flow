package com.example.ui.editor

class UndoRedoManager(
    private val maxHistorySize: Int = 50
) {
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    val canUndo: Boolean get() = undoStack.size > 1
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun initialize(initialText: String) {
        undoStack.clear()
        redoStack.clear()
        undoStack.addLast(initialText)
    }

    fun pushState(newText: String) {
        if (undoStack.isNotEmpty() && undoStack.last() == newText) {
            return
        }
        undoStack.addLast(newText)
        if (undoStack.size > maxHistorySize) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    fun undo(): String? {
        if (!canUndo) return null
        val current = undoStack.removeLast()
        redoStack.addLast(current)
        return undoStack.lastOrNull()
    }

    fun redo(): String? {
        if (!canRedo) return null
        val next = redoStack.removeLast()
        undoStack.addLast(next)
        return next
    }
}

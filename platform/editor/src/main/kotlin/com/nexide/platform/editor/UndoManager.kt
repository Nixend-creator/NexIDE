/*
 * Copyright 2026 NexIDE Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexide.platform.editor

/**
 * Simple undo/redo manager based on text snapshots.
 * Sufficient for the current Document model.
 */
class UndoManager(
  private val document: Document,
  private val maxHistorySize: Int = 100
) {
  private val myUndoStack = ArrayDeque<String>()
  private val myRedoStack = ArrayDeque<String>()
  private var myIsPerformingUndoRedo = false
  private var myLastRecordedText: String = document.getText()

  val canUndo: Boolean
    get() = myUndoStack.isNotEmpty()

  val canRedo: Boolean
    get() = myRedoStack.isNotEmpty()

  /**
   * Call after a user-initiated change to record history.
   */
  fun recordChange() {
    if (myIsPerformingUndoRedo) return
    val current = document.getText()
    if (current == myLastRecordedText) return

    myUndoStack.addLast(myLastRecordedText)
    while (myUndoStack.size > maxHistorySize) {
      myUndoStack.removeFirst()
    }
    myRedoStack.clear()
    myLastRecordedText = current
  }

  fun undo(): Boolean {
    if (!canUndo) return false
    myIsPerformingUndoRedo = true
    try {
      myRedoStack.addLast(document.getText())
      val previous = myUndoStack.removeLast()
      document.setText(previous)
      myLastRecordedText = previous
      return true
    } finally {
      myIsPerformingUndoRedo = false
    }
  }

  fun redo(): Boolean {
    if (!canRedo) return false
    myIsPerformingUndoRedo = true
    try {
      myUndoStack.addLast(document.getText())
      val next = myRedoStack.removeLast()
      document.setText(next)
      myLastRecordedText = next
      return true
    } finally {
      myIsPerformingUndoRedo = false
    }
  }
}

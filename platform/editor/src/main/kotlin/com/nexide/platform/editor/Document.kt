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
 * Mutable text document used by the editor.
 * Analogous to com.intellij.openapi.editor.Document.
 *
 * This is a simplified version — no markers, folds or bulk updates yet.
 */
class Document(initialText: String = "") {

  private var myText: String = initialText
  private val myListeners = mutableListOf<DocumentListener>()

  val textLength: Int
    get() = myText.length

  fun getText(): String = myText

  fun getText(startOffset: Int, endOffset: Int): String {
    require(startOffset in 0..myText.length)
    require(endOffset in startOffset..myText.length)
    return myText.substring(startOffset, endOffset)
  }

  /**
   * Replaces the entire content.
   */
  fun setText(newText: String) {
    if (myText == newText) return
    val oldText = myText
    myText = newText
    fireDocumentChanged(0, oldText.length, newText)
  }

  /**
   * Replaces a range of text.
   */
  fun replaceString(startOffset: Int, endOffset: Int, newFragment: String) {
    require(startOffset in 0..myText.length)
    require(endOffset in startOffset..myText.length)

    if (startOffset == endOffset && newFragment.isEmpty()) return

    val oldText = myText
    myText = oldText.substring(0, startOffset) + newFragment + oldText.substring(endOffset)
    fireDocumentChanged(startOffset, endOffset - startOffset, newFragment)
  }

  fun insertString(offset: Int, text: String) {
    replaceString(offset, offset, text)
  }

  fun deleteString(startOffset: Int, endOffset: Int) {
    replaceString(startOffset, endOffset, "")
  }

  fun addDocumentListener(listener: DocumentListener) {
    myListeners.add(listener)
  }

  fun removeDocumentListener(listener: DocumentListener) {
    myListeners.remove(listener)
  }

  private fun fireDocumentChanged(offset: Int, oldLength: Int, newFragment: String) {
    val event = DocumentEvent(this, offset, oldLength, newFragment)
    myListeners.toList().forEach { it.documentChanged(event) }
  }
}

/**
 * Listener for document changes.
 */
fun interface DocumentListener {
  fun documentChanged(event: DocumentEvent)
}

/**
 * Describes a change in a [Document].
 */
data class DocumentEvent(
  val document: Document,
  val offset: Int,
  val oldLength: Int,
  val newFragment: String
) {
  val newLength: Int
    get() = newFragment.length
}

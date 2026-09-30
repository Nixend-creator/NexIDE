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
 * Auto-insert closing pair when typing an opener. Returns null if no change.
 *
 * @param oldText previous document text
 * @param newText text after the edit
 * @param newCaret caret after the edit (usually selection.start)
 */
object AutoPair {

  private val PAIRS = mapOf(
    '(' to ')',
    '[' to ']',
    '{' to '}',
    '"' to '"',
    '\'' to '\''
  )

  /**
   * If the user just typed a single opening character, insert its closer
   * and place the caret between the pair.
   *
   * @return Pair(text, caret) or null
   */
  fun apply(oldText: String, newText: String, newCaret: Int): Pair<String, Int>? {
    // Only single-character insertions
    if (newText.length != oldText.length + 1) return null
    if (newCaret < 1 || newCaret > newText.length) return null

    val inserted = newText[newCaret - 1]
    val closer = PAIRS[inserted] ?: return null

    // Skip if deleting/replacing multi-char selection was involved — length check above handles insert only
    // Don't pair quotes if previous char is a letter/digit (don't break words: it's)
    if ((inserted == '\'' || inserted == '"') && newCaret >= 2) {
      val prev = newText[newCaret - 2]
      if (prev.isLetterOrDigit() || prev == '_') return null
    }

    // If next char is already the closer, don't double
    if (newCaret < newText.length && newText[newCaret] == closer) {
      return null
    }

    val withPair = newText.substring(0, newCaret) + closer + newText.substring(newCaret)
    return withPair to newCaret // caret stays between opener and closer
  }
}

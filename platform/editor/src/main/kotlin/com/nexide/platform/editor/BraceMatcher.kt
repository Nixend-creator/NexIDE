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
 * Finds matching brace pairs around the caret for highlight.
 */
object BraceMatcher {

  private val OPEN = setOf('(', '[', '{')
  private val CLOSE = setOf(')', ']', '}')
  private val PAIR = mapOf(
    '(' to ')',
    '[' to ']',
    '{' to '}',
    ')' to '(',
    ']' to '[',
    '}' to '{'
  )

  /**
   * @return pair of offsets (open, close) inclusive, or null if no match
   */
  fun findPair(text: String, caret: Int): Pair<Int, Int>? {
    if (text.isEmpty() || caret < 0 || caret > text.length) return null

    // Prefer char at caret, else char before caret
    val candidates = buildList {
      if (caret < text.length) add(caret)
      if (caret > 0) add(caret - 1)
    }.distinct()

    for (pos in candidates) {
      val ch = text[pos]
      when {
        ch in OPEN -> {
          val match = scanForward(text, pos, ch, PAIR[ch]!!) ?: continue
          return pos to match
        }
        ch in CLOSE -> {
          val match = scanBackward(text, pos, ch, PAIR[ch]!!) ?: continue
          return match to pos
        }
      }
    }
    return null
  }

  private fun scanForward(text: String, from: Int, open: Char, close: Char): Int? {
    var depth = 0
    for (i in from until text.length) {
      when (text[i]) {
        open -> depth++
        close -> {
          depth--
          if (depth == 0) return i
        }
      }
    }
    return null
  }

  private fun scanBackward(text: String, from: Int, close: Char, open: Char): Int? {
    var depth = 0
    for (i in from downTo 0) {
      when (text[i]) {
        close -> depth++
        open -> {
          depth--
          if (depth == 0) return i
        }
      }
    }
    return null
  }
}

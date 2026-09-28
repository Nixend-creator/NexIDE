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

package com.nexide.platform.editor.syntax

/**
 * Lightweight tokenizer for keywords, strings, comments and numbers.
 */
object SimpleTokenizer {

  fun tokenize(text: String, keywords: Set<String>): List<HighlightSpan> {
    val spans = mutableListOf<HighlightSpan>()
    var i = 0
    val n = text.length

    while (i < n) {
      val c = text[i]

      // Line comment //
      if (c == '/' && i + 1 < n && text[i + 1] == '/') {
        val start = i
        i += 2
        while (i < n && text[i] != '\n') i++
        spans.add(HighlightSpan(start, i, HighlightTokenType.COMMENT))
        continue
      }

      // Block comment /* */
      if (c == '/' && i + 1 < n && text[i + 1] == '*') {
        val start = i
        i += 2
        while (i + 1 < n && !(text[i] == '*' && text[i + 1] == '/')) i++
        if (i + 1 < n) i += 2 else i = n
        spans.add(HighlightSpan(start, i, HighlightTokenType.COMMENT))
        continue
      }

      // String "..." or """..."""
      if (c == '"') {
        val start = i
        if (i + 2 < n && text[i + 1] == '"' && text[i + 2] == '"') {
          // Triple-quoted string
          i += 3
          while (i + 2 < n && !(text[i] == '"' && text[i + 1] == '"' && text[i + 2] == '"')) i++
          if (i + 2 < n) i += 3 else i = n
        } else {
          i++
          while (i < n && text[i] != '"' && text[i] != '\n') {
            if (text[i] == '\\' && i + 1 < n) i += 2 else i++
          }
          if (i < n && text[i] == '"') i++
        }
        spans.add(HighlightSpan(start, i, HighlightTokenType.STRING))
        continue
      }

      // Char '...'
      if (c == '\'') {
        val start = i
        i++
        while (i < n && text[i] != '\'' && text[i] != '\n') {
          if (text[i] == '\\' && i + 1 < n) i += 2 else i++
        }
        if (i < n && text[i] == '\'') i++
        spans.add(HighlightSpan(start, i, HighlightTokenType.STRING))
        continue
      }

      // Number
      if (c.isDigit()) {
        val start = i
        while (i < n && (text[i].isDigit() || text[i] == '.' || text[i] == '_' ||
            text[i] == 'x' || text[i] == 'X' || text[i] in 'a'..'f' || text[i] in 'A'..'F' ||
            text[i] == 'L' || text[i] == 'f' || text[i] == 'F' || text[i] == 'u' || text[i] == 'U')
        ) {
          i++
        }
        spans.add(HighlightSpan(start, i, HighlightTokenType.NUMBER))
        continue
      }

      // Identifier / keyword
      if (c.isLetter() || c == '_' || c == '`') {
        val start = i
        if (c == '`') {
          i++
          while (i < n && text[i] != '`') i++
          if (i < n) i++
        } else {
          while (i < n && (text[i].isLetterOrDigit() || text[i] == '_')) i++
        }
        val word = text.substring(start, i)
        if (word in keywords) {
          spans.add(HighlightSpan(start, i, HighlightTokenType.KEYWORD))
        }
        continue
      }

      i++
    }

    return spans
  }
}

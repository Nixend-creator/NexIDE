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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Token type used for syntax highlighting.
 */
enum class HighlightTokenType {
  KEYWORD,
  STRING,
  COMMENT,
  NUMBER,
  DEFAULT
}

/**
 * A highlighted span in the text.
 */
data class HighlightSpan(
  val start: Int,
  val end: Int,
  val type: HighlightTokenType
)

/**
 * Produces highlight spans for a given language.
 */
interface SyntaxHighlighter {
  val language: String
  fun highlight(text: String): List<HighlightSpan>
}

/**
 * Registry of syntax highlighters by language id.
 */
object SyntaxHighlighterRegistry {
  private val myHighlighters = mutableMapOf<String, SyntaxHighlighter>()

  init {
    register(KotlinSyntaxHighlighter)
    register(JavaSyntaxHighlighter)
  }

  fun register(highlighter: SyntaxHighlighter) {
    myHighlighters[highlighter.language.lowercase()] = highlighter
  }

  fun getHighlighter(language: String): SyntaxHighlighter? {
    return myHighlighters[language.lowercase()]
  }

  fun highlightAsAnnotatedString(
    text: String,
    language: String,
    colors: SyntaxColors
  ): AnnotatedString {
    val highlighter = getHighlighter(language)
    if (highlighter == null || text.isEmpty()) {
      return AnnotatedString(text)
    }

    val spans = highlighter.highlight(text).sortedBy { it.start }
    return buildAnnotatedString {
      var cursor = 0
      for (span in spans) {
        if (span.start > cursor) {
          append(text.substring(cursor, span.start))
        }
        val end = span.end.coerceAtMost(text.length)
        if (span.start < end) {
          withStyle(SpanStyle(color = colors.colorFor(span.type))) {
            append(text.substring(span.start, end))
          }
        }
        cursor = end
      }
      if (cursor < text.length) {
        append(text.substring(cursor))
      }
    }
  }
}

/**
 * Color scheme for syntax tokens.
 */
data class SyntaxColors(
  val keyword: Color = Color(0xFFCC7832),
  val string: Color = Color(0xFF6A8759),
  val comment: Color = Color(0xFF808080),
  val number: Color = Color(0xFF6897BB),
  val default: Color = Color(0xFFA9B7C6)
) {
  fun colorFor(type: HighlightTokenType): Color = when (type) {
    HighlightTokenType.KEYWORD -> keyword
    HighlightTokenType.STRING -> string
    HighlightTokenType.COMMENT -> comment
    HighlightTokenType.NUMBER -> number
    HighlightTokenType.DEFAULT -> default
  }

  companion object {
    val Darcula = SyntaxColors()
    val Light = SyntaxColors(
      keyword = Color(0xFF0000FF),
      string = Color(0xFF008000),
      comment = Color(0xFF808080),
      number = Color(0xFF098658),
      default = Color(0xFF000000)
    )
  }
}

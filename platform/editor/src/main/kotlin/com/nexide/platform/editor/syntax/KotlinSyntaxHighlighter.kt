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
 * Simple keyword/string/comment highlighter for Kotlin.
 */
object KotlinSyntaxHighlighter : SyntaxHighlighter {
  override val language: String = "kotlin"

  private val keywords = setOf(
    "as", "as?", "break", "class", "continue", "do", "else", "false", "for",
    "fun", "if", "in", "!in", "interface", "is", "!is", "null", "object",
    "package", "return", "super", "this", "throw", "true", "try", "typealias",
    "typeof", "val", "var", "when", "while", "by", "catch", "constructor",
    "delegate", "dynamic", "field", "file", "finally", "get", "import",
    "init", "param", "property", "receiver", "set", "setparam", "where",
    "actual", "abstract", "annotation", "companion", "const", "crossinline",
    "data", "enum", "expect", "external", "final", "infix", "inline",
    "inner", "internal", "lateinit", "noinline", "open", "operator",
    "out", "override", "private", "protected", "public", "reified",
    "sealed", "suspend", "tailrec", "value", "vararg"
  )

  override fun highlight(text: String): List<HighlightSpan> {
    return SimpleTokenizer.tokenize(text, keywords)
  }
}

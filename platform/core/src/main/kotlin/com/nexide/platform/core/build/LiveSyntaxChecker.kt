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

package com.nexide.platform.core.build

/**
 * Lightweight on-type checks (not a real compiler).
 * Detects unmatched braces and unclosed string literals.
 */
object LiveSyntaxChecker {

  fun check(filePath: String, text: String): List<Problem> {
    if (text.isEmpty()) return emptyList()
    return checkBracesAndQuotes(filePath, text)
  }

  private fun checkBracesAndQuotes(filePath: String, text: String): List<Problem> {
    val out = mutableListOf<Problem>()
    val stack = ArrayDeque<Pair<Char, Int>>()
    var i = 0
    var inLineComment = false
    var inBlockComment = false
    var inString = false
    var stringQuote = ' '
    var stringStart = 0

    fun lineOf(offset: Int): Int =
      text.substring(0, offset.coerceIn(0, text.length)).count { it == '\n' } + 1

    while (i < text.length) {
      val c = text[i]
      if (inLineComment) {
        if (c == '\n') inLineComment = false
        i++
        continue
      }
      if (inBlockComment) {
        if (c == '*' && i + 1 < text.length && text[i + 1] == '/') {
          inBlockComment = false
          i += 2
        } else {
          i++
        }
        continue
      }
      if (inString) {
        if (c == '\\' && i + 1 < text.length) {
          i += 2
          continue
        }
        if (c == stringQuote) {
          inString = false
        }
        i++
        continue
      }
      when {
        c == '/' && i + 1 < text.length && text[i + 1] == '/' -> {
          inLineComment = true
          i += 2
        }
        c == '/' && i + 1 < text.length && text[i + 1] == '*' -> {
          inBlockComment = true
          i += 2
        }
        c == '"' || c == '\'' -> {
          inString = true
          stringQuote = c
          stringStart = i
          i++
        }
        c == '(' || c == '[' || c == '{' -> {
          stack.addLast(c to i)
          i++
        }
        c == ')' || c == ']' || c == '}' -> {
          val want = when (c) {
            ')' -> '('
            ']' -> '['
            else -> '{'
          }
          if (stack.isEmpty() || stack.last().first != want) {
            out.add(
              Problem(ProblemSeverity.ERROR, "Unmatched '$c'", filePath, lineOf(i))
            )
          } else {
            stack.removeLast()
          }
          i++
        }
        else -> i++
      }
    }
    if (inString) {
      out.add(
        Problem(ProblemSeverity.ERROR, "Unclosed string literal", filePath, lineOf(stringStart))
      )
    }
    while (stack.isNotEmpty()) {
      val (ch, off) = stack.removeLast()
      out.add(
        Problem(ProblemSeverity.ERROR, "Unclosed '$ch'", filePath, lineOf(off))
      )
    }
    return out
  }
}

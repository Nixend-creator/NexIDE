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

import com.nexide.platform.core.index.SymbolIndex

/**
 * Lightweight text-based inspections (not a real compiler front-end).
 */
object SimpleInspections {

  private val IMPORT_KT = Regex("""^\s*import\s+([\w.]+)(?:\s+as\s+(\w+))?\s*$""")
  private val IMPORT_JAVA = Regex("""^\s*import\s+(static\s+)?([\w.]+)\s*;\s*$""")

  private val KEYWORDS = setOf(
    "if", "else", "when", "for", "while", "do", "return", "break", "continue",
    "fun", "val", "var", "class", "interface", "object", "package", "import",
    "true", "false", "null", "this", "super", "is", "as", "in", "out",
    "public", "private", "protected", "internal", "open", "override", "final",
    "data", "sealed", "enum", "companion", "init", "constructor", "get", "set",
    "try", "catch", "finally", "throw", "typeof", "new", "extends", "implements"
  )

  /**
   * Unused import: imported simple name never appears elsewhere in the file.
   */
  fun unusedImports(filePath: String, text: String): List<Problem> {
    val lines = text.lines()
    val problems = mutableListOf<Problem>()
    val body = StringBuilder()
    lines.forEachIndexed { idx, line ->
      val imp = parseImport(line)
      if (imp == null) {
        body.append(line).append('\n')
      } else {
        val (simple, _) = imp
        // Search body (and other non-this-import lines roughly): whole text without this line
        val used = text.lineSequence()
          .filterIndexed { i, _ -> i != idx }
          .any { other ->
            Regex("""\b${Regex.escape(simple)}\b""").containsMatchIn(other)
          }
        if (!used) {
          problems.add(
            Problem(
              severity = ProblemSeverity.WARNING,
              message = "Unused import '$simple'",
              filePath = filePath,
              line = idx + 1,
              rawLine = line
            )
          )
        }
      }
    }
    return problems
  }

  private fun parseImport(line: String): Pair<String, String>? {
    IMPORT_KT.matchEntire(line.trim())?.let { m ->
      val full = m.groupValues[1]
      val alias = m.groupValues[2]
      val simple = if (alias.isNotBlank()) alias else full.substringAfterLast('.')
      if (simple == "*" || simple.isBlank()) return null
      return simple to full
    }
    IMPORT_JAVA.matchEntire(line.trim())?.let { m ->
      val full = m.groupValues[2]
      val simple = full.substringAfterLast('.')
      if (simple == "*" || simple.isBlank()) return null
      return simple to full
    }
    return null
  }

  /**
   * Soft unresolved type-like identifiers: Capitalized words not in SymbolIndex
   * and not declared in the current file. High false-positive risk — WARNING only.
   */
  fun softUnresolvedTypes(filePath: String, text: String): List<Problem> {
    val localDecls = Regex(
      """\b(?:class|interface|object|enum\s+class|data\s+class|sealed\s+class|typealias)\s+([A-Z][A-Za-z0-9_]*)"""
    ).findAll(text).map { it.groupValues[1] }.toSet()

    val typeLike = Regex("""\b([A-Z][A-Za-z0-9_]{2,})\b""")
    val problems = mutableListOf<Problem>()
    val seen = mutableSetOf<String>()

    text.lines().forEachIndexed { idx, line ->
      val trimmed = line.trim()
      if (trimmed.startsWith("import ") || trimmed.startsWith("package ") ||
        trimmed.startsWith("//") || trimmed.startsWith("*")
      ) {
        return@forEachIndexed
      }
      typeLike.findAll(line).forEach { m ->
        val name = m.groupValues[1]
        if (name in KEYWORDS || name in localDecls) return@forEach
        if (name.startsWith("ERROR") || name.endsWith("Exception") && name.length < 12) {
          // still check index
        }
        // Skip ALL_CAPS constants
        if (name.all { it.isUpperCase() || it.isDigit() || it == '_' }) return@forEach
        if (SymbolIndex.exact(name).isNotEmpty()) return@forEach
        val key = "$name:${idx + 1}"
        if (key in seen) return@forEach
        seen.add(key)
        // Only report first occurrence of each unresolved name
        if (seen.count { it.startsWith("$name:") } > 1) return@forEach
        if (problems.any { it.message.contains("'$name'") }) return@forEach
        problems.add(
          Problem(
            severity = ProblemSeverity.WARNING,
            message = "Unresolved symbol '$name' (index)",
            filePath = filePath,
            line = idx + 1,
            rawLine = line
          )
        )
      }
    }
    return problems.take(20)
  }

  fun checkFile(filePath: String, text: String, includeSoftResolve: Boolean = true): List<Problem> {
    val list = mutableListOf<Problem>()
    list.addAll(unusedImports(filePath, text))
    if (includeSoftResolve && (filePath.endsWith(".kt") || filePath.endsWith(".java"))) {
      list.addAll(softUnresolvedTypes(filePath, text))
    }
    return list
  }
}

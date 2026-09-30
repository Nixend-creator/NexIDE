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

package com.nexide.platform.editor.completion

import com.nexide.platform.core.index.SymbolIndex

/**
 * A single completion suggestion.
 */
data class LookupElement(
  val lookupString: String,
  val typeText: String = "",
  val priority: Int = 0
)

/**
 * Builds basic completion variants from keywords + identifiers in the current file.
 */
object CompletionContributor {

  private val KOTLIN_KEYWORDS = listOf(
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
    "if", "in", "interface", "is", "null", "object", "package", "return",
    "super", "this", "throw", "true", "try", "typealias", "typeof", "val",
    "var", "when", "while", "by", "catch", "constructor", "delegate",
    "dynamic", "field", "file", "finally", "get", "import", "init",
    "param", "property", "receiver", "set", "setparam", "where",
    "actual", "abstract", "annotation", "companion", "const", "crossinline",
    "data", "enum", "expect", "external", "final", "infix", "inline",
    "inner", "internal", "lateinit", "noinline", "open", "operator",
    "out", "override", "private", "protected", "public", "reified",
    "sealed", "suspend", "tailrec", "value", "vararg"
  )

  private val JAVA_KEYWORDS = listOf(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch",
    "char", "class", "const", "continue", "default", "do", "double",
    "else", "enum", "extends", "final", "finally", "float", "for",
    "goto", "if", "implements", "import", "instanceof", "int",
    "interface", "long", "native", "new", "package", "private",
    "protected", "public", "return", "short", "static", "strictfp",
    "super", "switch", "synchronized", "this", "throw", "throws",
    "transient", "try", "void", "volatile", "while", "true", "false", "null"
  )

  private val IDENTIFIER = Regex("""[A-Za-z_][A-Za-z0-9_]*""")

  /**
   * Prefix is the identifier being typed at [offset] (may be empty).
   */
  fun prefixAt(text: String, offset: Int): String {
    val safe = offset.coerceIn(0, text.length)
    var start = safe
    while (start > 0) {
      val c = text[start - 1]
      if (c.isLetterOrDigit() || c == '_') start-- else break
    }
    return text.substring(start, safe)
  }

  fun complete(text: String, offset: Int, language: String, projectRoot: String? = null): List<LookupElement> {
    val prefix = prefixAt(text, offset)
    val prefixLower = prefix.lowercase()
    val result = mutableListOf<LookupElement>()

    val keywords = when (language.lowercase()) {
      "kotlin", "kts" -> KOTLIN_KEYWORDS
      "java" -> JAVA_KEYWORDS
      else -> KOTLIN_KEYWORDS + JAVA_KEYWORDS
    }

    for (kw in keywords) {
      if (prefix.isEmpty() || kw.startsWith(prefixLower) || kw.startsWith(prefix)) {
        result.add(LookupElement(kw, "keyword", priority = 10))
      }
    }


    // Project symbol index
    if (projectRoot != null) {
      try {
        SymbolIndex.ensureIndexed(java.io.File(projectRoot))
        for (sym in SymbolIndex.lookup(prefix, 30)) {
          result.add(LookupElement(sym.name, "${sym.kind} · ${java.io.File(sym.filePath).name}", priority = 8))
        }
      } catch (_: Exception) {
      }
    }

    // Identifiers from current file
    val seen = mutableSetOf<String>()
    for (match in IDENTIFIER.findAll(text)) {
      val word = match.value
      if (word.length < 2) continue
      if (word == prefix) continue
      if (prefix.isNotEmpty() && !word.startsWith(prefix) && !word.lowercase().startsWith(prefixLower)) {
        continue
      }
      if (seen.add(word)) {
        result.add(LookupElement(word, "symbol", priority = 5))
      }
    }

    return result
      .sortedWith(compareByDescending<LookupElement> { it.priority }.thenBy { it.lookupString.lowercase() })
      .distinctBy { it.lookupString }
      .take(40)
  }
}

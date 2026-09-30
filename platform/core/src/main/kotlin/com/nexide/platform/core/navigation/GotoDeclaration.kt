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

package com.nexide.platform.core.navigation

import com.nexide.platform.core.index.SymbolIndex
import java.io.File

/**
 * A navigation target for Go to Declaration / Find Usages.
 */
data class NavigationTarget(
  val filePath: String,
  val line: Int,
  val column: Int = 1,
  val matchText: String = "",
  val kind: String = "declaration" // declaration | usage
)

/**
 * Heuristic navigation: finds symbol definitions and usages by text patterns.
 * Not a full PSI resolve — good enough for same-project jump-to-name.
 */
object GotoDeclaration {

  private val IDENTIFIER = Regex("""[A-Za-z_][A-Za-z0-9_]*""")

  private val DECLARATION_PATTERNS = listOf(
    // Kotlin / Java style
    Regex("""\b(?:fun|class|interface|object|enum\s+class|val|var|typealias|data\s+class|sealed\s+class|annotation\s+class)\s+([A-Za-z_][A-Za-z0-9_]*)"""),
    Regex("""\b(?:public|private|protected|internal|static|final|abstract)?\s*(?:class|interface|enum|void|int|long|boolean|String|double|float)\s+([A-Za-z_][A-Za-z0-9_]*)"""),
    Regex("""\b(?:fun\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*\("""), // function-like
  )

  private val TEXT_EXTENSIONS = setOf(
    "kt", "kts", "java", "groovy", "gradle", "properties", "xml", "md", "txt"
  )

  /**
   * Identifier under [offset] in [text], or null.
   */
  fun symbolAt(text: String, offset: Int): String? {
    if (text.isEmpty()) return null
    val safe = offset.coerceIn(0, text.length)
    // If caret is at end of identifier, still capture it
    var pos = safe
    if (pos > 0 && pos == text.length || (pos < text.length && !isIdentChar(text[pos]))) {
      pos = (pos - 1).coerceAtLeast(0)
    }
    if (pos >= text.length || !isIdentChar(text[pos])) return null
    var start = pos
    var end = pos + 1
    while (start > 0 && isIdentChar(text[start - 1])) start--
    while (end < text.length && isIdentChar(text[end])) end++
    val word = text.substring(start, end)
    return word.takeIf { it.isNotEmpty() && !isKeyword(word) }
  }

  /**
   * Search project tree for declarations of [symbol]. Prefer declaration-like lines.
   */
  fun findDeclarations(
    projectRoot: File,
    symbol: String,
    maxResults: Int = 30
  ): List<NavigationTarget> {
    if (symbol.isBlank() || !projectRoot.isDirectory) return emptyList()
    // Prefer symbol index (fast path)
    try {
      SymbolIndex.ensureIndexed(projectRoot)
      val indexed = SymbolIndex.exact(symbol)
      if (indexed.isNotEmpty()) {
        return indexed.take(maxResults).map {
          NavigationTarget(
            filePath = it.filePath,
            line = it.line,
            column = 1,
            matchText = "${it.kind} ${it.name}",
            kind = "declaration"
          )
        }
      }
    } catch (_: Exception) {
    }
    val declarations = mutableListOf<NavigationTarget>()
    val usages = mutableListOf<NavigationTarget>()
    val symbolRegex = Regex("""\b${Regex.escape(symbol)}\b""")

    walkSources(projectRoot) { file ->
      if (declarations.size + usages.size >= maxResults * 2) return@walkSources
      val lines = try {
        file.readLines(Charsets.UTF_8)
      } catch (_: Exception) {
        return@walkSources
      }
      lines.forEachIndexed { index, line ->
        if (!symbolRegex.containsMatchIn(line)) return@forEachIndexed
        val lineNo = index + 1
        val col = (line.indexOf(symbol) + 1).coerceAtLeast(1)
        val isDecl = looksLikeDeclaration(line, symbol)
        val target = NavigationTarget(
          filePath = file.absolutePath,
          line = lineNo,
          column = col,
          matchText = line.trim().take(120),
          kind = if (isDecl) "declaration" else "usage"
        )
        if (isDecl) declarations.add(target) else usages.add(target)
      }
    }

    // Declarations first, then a few usages as fallback
    return (declarations + usages).take(maxResults)
  }

  fun findUsages(projectRoot: File, symbol: String, maxResults: Int = 50): List<NavigationTarget> {
    return findDeclarations(projectRoot, symbol, maxResults).map {
      it.copy(kind = if (it.kind == "declaration") "declaration" else "usage")
    }
  }

  private fun looksLikeDeclaration(line: String, symbol: String): Boolean {
    val trimmed = line.trim()
    if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
      return false
    }
    for (pattern in DECLARATION_PATTERNS) {
      val m = pattern.find(trimmed) ?: continue
      if (m.groupValues.getOrNull(1) == symbol) return true
    }
    // "symbol =" assignment at statement start
    if (Regex("""^(?:val|var|const\s+val)\s+$symbol\b""").containsMatchIn(trimmed)) return true
    return false
  }

  private fun walkSources(root: File, visitor: (File) -> Unit) {
    val skip = setOf(
      ".git", ".gradle", "build", "out", "node_modules", ".idea", "captures"
    )
    root.walkTopDown()
      .onEnter { dir -> dir.name !in skip }
      .filter { it.isFile && it.extension.lowercase() in TEXT_EXTENSIONS }
      .filter { it.length() < 2_000_000 }
      .forEach(visitor)
  }

  private fun isIdentChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'

  private val KEYWORDS = setOf(
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
    "if", "in", "interface", "is", "null", "object", "package", "return",
    "super", "this", "throw", "true", "try", "val", "var", "when", "while",
    "import", "public", "private", "protected", "internal", "override",
    "open", "abstract", "final", "static", "void", "int", "new", "return"
  )

  private fun isKeyword(word: String): Boolean = word.lowercase() in KEYWORDS
}

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

package com.nexide.platform.psi.structure

/**
 * Kind of a structure element extracted from source.
 */
enum class StructureKind {
  CLASS,
  OBJECT,
  INTERFACE,
  FUNCTION,
  PROPERTY,
  ENUM,
  OTHER
}

/**
 * One navigable structure entry (1-based line).
 */
data class StructureItem(
  val name: String,
  val kind: StructureKind,
  val line: Int,
  val detail: String = ""
)

/**
 * Lightweight structure extractor for Kotlin / Java sources.
 * Regex-based — not a full parser; good enough for navigation.
 */
object FileStructureExtractor {

  private val kotlinPatterns = listOf(
    Regex("""^\s*(?:(?:public|private|internal|protected|open|abstract|sealed|data|inner|enum|annotation|inline|value)\s+)*(class|object|interface|enum\s+class)\s+(\w+)"""),
    Regex("""^\s*(?:(?:public|private|internal|protected|open|abstract|override|suspend|inline|tailrec|operator|infix|external|actual|expect)\s+)*fun\s+(?:<[^>]+>\s+)?(\w+)"""),
    Regex("""^\s*(?:(?:public|private|internal|protected|open|override|const|lateinit|actual|expect)\s+)*(val|var)\s+(\w+)""")
  )

  private val javaPatterns = listOf(
    Regex("""^\s*(?:(?:public|private|protected|static|final|abstract|sealed)\s+)*(class|interface|enum|record)\s+(\w+)"""),
    Regex("""^\s*(?:(?:public|private|protected|static|final|abstract|synchronized|native|default)\s+)+(?:[\w.<>,\[\]\s]+?\s)+(\w+)\s*\(""")
  )

  fun extract(text: String, language: String): List<StructureItem> {
    val lang = language.lowercase()
    return when {
      lang == "kotlin" || lang == "kts" -> extractKotlin(text)
      lang == "java" -> extractJava(text)
      else -> emptyList()
    }
  }

  private fun extractKotlin(text: String): List<StructureItem> {
    val items = mutableListOf<StructureItem>()
    val lines = text.split('\n')
    for ((index, line) in lines.withIndex()) {
      val lineNo = index + 1
      // Skip comments
      val trimmed = line.trimStart()
      if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) continue

      val typeMatch = kotlinPatterns[0].find(line)
      if (typeMatch != null) {
        val kindWord = typeMatch.groupValues[1].replace("enum class", "enum").trim()
        val name = typeMatch.groupValues[2]
        val kind = when (kindWord) {
          "class" -> StructureKind.CLASS
          "object" -> StructureKind.OBJECT
          "interface" -> StructureKind.INTERFACE
          "enum" -> StructureKind.ENUM
          else -> StructureKind.OTHER
        }
        items.add(StructureItem(name, kind, lineNo, kindWord))
        continue
      }

      val funMatch = kotlinPatterns[1].find(line)
      if (funMatch != null) {
        val name = funMatch.groupValues[1]
        if (name !in setOf("if", "when", "for", "while", "try", "catch")) {
          items.add(StructureItem(name, StructureKind.FUNCTION, lineNo, "fun"))
        }
        continue
      }

      val propMatch = kotlinPatterns[2].find(line)
      if (propMatch != null) {
        val keyword = propMatch.groupValues[1]
        val name = propMatch.groupValues[2]
        items.add(StructureItem(name, StructureKind.PROPERTY, lineNo, keyword))
      }
    }
    return items
  }

  private fun extractJava(text: String): List<StructureItem> {
    val items = mutableListOf<StructureItem>()
    val lines = text.split('\n')
    for ((index, line) in lines.withIndex()) {
      val lineNo = index + 1
      val trimmed = line.trimStart()
      if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) continue

      val typeMatch = javaPatterns[0].find(line)
      if (typeMatch != null) {
        val kindWord = typeMatch.groupValues[1]
        val name = typeMatch.groupValues[2]
        val kind = when (kindWord) {
          "class", "record" -> StructureKind.CLASS
          "interface" -> StructureKind.INTERFACE
          "enum" -> StructureKind.ENUM
          else -> StructureKind.OTHER
        }
        items.add(StructureItem(name, kind, lineNo, kindWord))
        continue
      }

      val methodMatch = javaPatterns[1].find(line)
      if (methodMatch != null) {
        val name = methodMatch.groupValues[1]
        if (name !in setOf("if", "for", "while", "switch", "catch", "return", "new")) {
          items.add(StructureItem(name, StructureKind.FUNCTION, lineNo, "method"))
        }
      }
    }
    return items
  }
}

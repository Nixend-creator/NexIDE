/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core.index

import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class SymbolInfo(
  val name: String,
  val filePath: String,
  val line: Int,
  val kind: String // class, fun, val, etc.
)

/**
 * Lightweight project symbol index for completion and resolve.
 */
object SymbolIndex {
  private val byName = ConcurrentHashMap<String, MutableList<SymbolInfo>>()
  private var rootPath: String? = null
  private val DECL = Regex(
    """\b(?:fun|class|interface|object|enum\s+class|val|var|typealias|data\s+class|sealed\s+class)\s+([A-Za-z_][A-Za-z0-9_]*)"""
  )

  fun invalidate() {
    byName.clear()
    rootPath = null
  }

  fun ensureIndexed(projectRoot: File) {
    val path = projectRoot.canonicalPath
    if (rootPath == path && byName.isNotEmpty()) return
    rebuild(projectRoot)
  }

  fun rebuild(projectRoot: File) {
    byName.clear()
    rootPath = projectRoot.canonicalPath
    val skip = setOf(".git", ".gradle", "build", "out", "node_modules", ".idea")
    projectRoot.walkTopDown()
      .onEnter { it.name !in skip }
      .filter { it.isFile && it.extension in setOf("kt", "kts", "java") }
      .filter { it.length() < 1_000_000 }
      .forEach { file ->
        try {
          file.readLines(Charsets.UTF_8).forEachIndexed { idx, line ->
            DECL.findAll(line).forEach { m ->
              val name = m.groupValues[1]
              val kind = when {
                line.contains("class") -> "class"
                line.contains("interface") -> "interface"
                line.contains("object") -> "object"
                line.contains("fun") -> "fun"
                line.contains("val") || line.contains("var") -> "property"
                else -> "symbol"
              }
              byName.getOrPut(name) { mutableListOf() }.add(
                SymbolInfo(name, file.absolutePath, idx + 1, kind)
              )
            }
          }
        } catch (_: Exception) {
        }
      }
  }

  fun lookup(prefix: String, limit: Int = 40): List<SymbolInfo> {
    if (prefix.isEmpty()) {
      return byName.values.flatten().take(limit)
    }
    val p = prefix.lowercase()
    return byName.entries
      .filter { it.key.lowercase().startsWith(p) }
      .flatMap { it.value }
      .take(limit)
  }

  fun exact(name: String): List<SymbolInfo> = byName[name].orEmpty()
}

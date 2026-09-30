/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core.history

import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class HistoryEntry(
  val timestampMs: Long,
  val label: String,
  val content: String
)

/**
 * In-memory + disk local history snapshots per file (on save).
 */
object LocalHistory {
  private val memory = ConcurrentHashMap<String, MutableList<HistoryEntry>>()
  private const val MAX = 30

  private fun dir(): File =
    File(System.getProperty("user.home"), ".nexide/localHistory").also { it.mkdirs() }

  fun record(filePath: String, content: String, label: String = "save") {
    val list = memory.getOrPut(filePath) { mutableListOf() }
    if (list.isNotEmpty() && list.last().content == content) return
    list.add(HistoryEntry(System.currentTimeMillis(), label, content))
    while (list.size > MAX) list.removeAt(0)
    // Persist last few to disk (simple)
    try {
      val key = filePath.hashCode().toUInt().toString(16)
      val f = File(dir(), "$key.txt")
      f.writeText(list.takeLast(5).joinToString("\n----\n") { "${it.timestampMs}|${it.label}\n${it.content}" })
    } catch (_: Exception) {
    }
  }

  fun entries(filePath: String): List<HistoryEntry> = memory[filePath].orEmpty().asReversed()

  fun contentAt(filePath: String, indexFromNewest: Int): String? {
    val list = memory[filePath] ?: return null
    val idx = list.lastIndex - indexFromNewest
    return list.getOrNull(idx)?.content
  }
}

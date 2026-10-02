/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core.build

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DiagnosticsMergeTest {

  @Test
  fun `merges and dedupes by location and message`() {
    val build = listOf(
      Problem(ProblemSeverity.ERROR, "compile error", "/a.kt", 1)
    )
    val live = listOf(
      Problem(ProblemSeverity.ERROR, "compile error", "/a.kt", 1),
      Problem(ProblemSeverity.ERROR, "Unclosed '{'", "/a.kt", 3)
    )
    val merged = DiagnosticsMerge.merge(build, live)
    assertEquals(2, merged.size)
  }

  @Test
  fun `liveForFiles runs checker`() {
    val live = DiagnosticsMerge.liveForFiles(listOf("/x.kt" to "fun f() {"))
    assertTrue(live.any { it.message.contains("Unclosed") })
  }
}

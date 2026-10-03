/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core.refactor

import com.nexide.platform.core.build.Problem
import com.nexide.platform.core.build.ProblemSeverity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IntentionSupportTest {

  @Test
  fun `removes unused import line`() {
    val text = """
      import foo.Bar
      import foo.Used
      fun f(x: Used) {}
    """.trimIndent()
    val problems = listOf(
      Problem(ProblemSeverity.WARNING, "Unused import 'Bar'", "/x.kt", 1)
    )
    val applied = IntentionSupport.applyFirst(text, 1, problems)
    assertNotNull(applied)
    assertFalse(applied!!.first.contains("Bar"))
    assertTrue(applied.first.contains("Used"))
    assertEquals("Remove unused import", applied.second)
  }

  @Test
  fun `inserts import after package`() {
    val text = """
      package demo
      fun f(x: Bar) {}
    """.trimIndent()
    val next = IntentionSupport.insertImport(text, "other.Bar", true)
    assertTrue(next.contains("import other.Bar"))
    assertTrue(next.indexOf("import") > next.indexOf("package"))
  }

  @Test
  fun `add import intention from missing import problem`() {
    val text = """
      package demo
      fun f(x: Bar) {}
    """.trimIndent()
    val problems = listOf(
      Problem(
        ProblemSeverity.WARNING,
        "Missing import for 'Bar' (other.Bar)",
        "/x.kt",
        2
      )
    )
    val actions = IntentionSupport.actionsForLine(text, 2, problems)
    assertTrue(actions.any { it.title.contains("Import") })
    val next = actions.first { it.title.contains("Import") }.apply(text)
    assertTrue(next.contains("import other.Bar"))
  }
}

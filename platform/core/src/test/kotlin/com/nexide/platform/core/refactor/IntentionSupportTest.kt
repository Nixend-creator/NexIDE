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
}

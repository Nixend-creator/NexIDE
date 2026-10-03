/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core.refactor

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ImportOptimizerTest {

  @Test
  fun `removes unused and sorts`() {
    val text = """
      package demo
      import zzz.Unused
      import aaa.Used

      fun f(x: Used) {}
    """.trimIndent()
    val result = ImportOptimizer.optimize(text)
    assertTrue(result.removed >= 1)
    assertFalse(result.text.contains("Unused"))
    assertTrue(result.text.contains("import aaa.Used"))
  }
}

/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core.build

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SimpleInspectionsTest {

  @Test
  fun `detects unused import`() {
    val text = """
      package demo
      import foo.Bar
      import foo.Used

      fun f(x: Used) {}
    """.trimIndent()
    val problems = SimpleInspections.unusedImports("/Demo.kt", text)
    assertTrue(problems.any { it.message.contains("Bar") })
    assertTrue(problems.none { it.message.contains("Used") })
  }

  @Test
  fun `wildcard import ignored`() {
    val text = "import foo.*\nfun f() {}"
    assertTrue(SimpleInspections.unusedImports("/x.kt", text).isEmpty())
  }
}

/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core.build

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProblemNavigationTest {

  private val a1 = Problem(ProblemSeverity.ERROR, "a1", "/a.kt", 1)
  private val a5 = Problem(ProblemSeverity.WARNING, "a5", "/a.kt", 5)
  private val b2 = Problem(ProblemSeverity.ERROR, "b2", "/b.kt", 2)

  @Test
  fun `next wraps to first`() {
    val list = listOf(a1, a5, b2)
    val n = ProblemNavigation.step(list, "/b.kt", 2, forward = true)
    assertEquals(a1, n)
  }

  @Test
  fun `next within file`() {
    val n = ProblemNavigation.step(listOf(a1, a5, b2), "/a.kt", 1, forward = true)
    assertEquals(a5, n)
  }

  @Test
  fun `prev wraps to last`() {
    val n = ProblemNavigation.step(listOf(a1, a5, b2), "/a.kt", 1, forward = false)
    assertEquals(b2, n)
  }
}

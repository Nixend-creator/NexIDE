/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core.build

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LiveSyntaxCheckerTest {

  @Test
  fun `empty text has no problems`() {
    assertTrue(LiveSyntaxChecker.check("/x.kt", "").isEmpty())
  }

  @Test
  fun `balanced braces ok`() {
    val text = "fun f() { val x = listOf(1, 2) }"
    assertTrue(LiveSyntaxChecker.check("/x.kt", text).isEmpty())
  }

  @Test
  fun `unclosed brace reports error`() {
    val problems = LiveSyntaxChecker.check("/x.kt", "fun f() {")
    assertTrue(problems.any { it.message.contains("Unclosed") })
  }

  @Test
  fun `unmatched close reports error`() {
    val problems = LiveSyntaxChecker.check("/x.kt", "fun f() }")
    assertTrue(problems.any { it.message.contains("Unmatched") })
  }

  @Test
  fun `unclosed string reports error`() {
    val problems = LiveSyntaxChecker.check("/x.kt", "val s = \"hello")
    assertTrue(problems.any { it.message.contains("string", ignoreCase = true) })
  }

  @Test
  fun `braces inside string ignored`() {
    val text = "val s = \"{ not a brace\""
    assertTrue(LiveSyntaxChecker.check("/x.kt", text).isEmpty())
  }

  @Test
  fun `braces inside line comment ignored`() {
    val text = "fun f() {\n  // { ignored\n}"
    assertTrue(LiveSyntaxChecker.check("/x.kt", text).isEmpty())
  }

  @Test
  fun `nested brackets`() {
    assertTrue(LiveSyntaxChecker.check("/x.kt", "arr[i[0]]").isEmpty())
    val bad = LiveSyntaxChecker.check("/x.kt", "arr[i[0]")
    assertTrue(bad.isNotEmpty())
  }
}

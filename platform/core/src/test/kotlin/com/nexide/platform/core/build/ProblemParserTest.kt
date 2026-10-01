/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core.build

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProblemParserTest {

  @Test
  fun `parses junit failed stack frame`() {
    val out = """
      AutoPairTest > pairs parentheses FAILED
          java.lang.AssertionError
              at com.nexide.platform.editor.AutoPairTest.pairs_parentheses(AutoPairTest.kt:12)
    """.trimIndent()
    val problems = ProblemParser.parseTestFailures(out)
    assertTrue(problems.isNotEmpty())
    assertEquals("AutoPairTest.kt", problems.first().filePath)
    assertEquals(12, problems.first().line)
    assertTrue(problems.first().message.contains("Test failed"))
  }
}

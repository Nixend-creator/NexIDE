/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core.run

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class GradleClasspathResolverTest {

  @Test
  fun `parses classpath markers`() {
    val jar1 = "/tmp/a.jar"
    val jar2 = "/tmp/b.jar"
    val out = """
      some gradle noise
      NEXIDE_CP_BEGIN
      $jar1${File.pathSeparator}$jar2
      NEXIDE_CP_END
      more noise
    """.trimIndent()
    val cp = GradleClasspathResolver.parseClasspathOutput(out)
    assertNotNull(cp)
    assertTrue(cp!!.contains("a.jar"))
    assertTrue(cp.contains("b.jar"))
  }

  @Test
  fun `returns null without markers`() {
    assertNull(GradleClasspathResolver.parseClasspathOutput("no markers here"))
  }
}

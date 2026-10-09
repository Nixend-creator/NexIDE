/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core.gradle

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class GradleModuleTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `findBuildFile prefers kts`() {
    val dir = tempDir.toFile()
    File(dir, "build.gradle.kts").writeText("// kts")
    File(dir, "build.gradle").writeText("// groovy")
    val mod = GradleModule(":lib", dir)
    assertEquals("build.gradle.kts", mod.findBuildFile()?.name)
  }

  @Test
  fun `findBuildFile groovy fallback`() {
    val dir = tempDir.toFile()
    File(dir, "build.gradle").writeText("// groovy")
    val mod = GradleModule(":lib", dir)
    assertEquals("build.gradle", mod.findBuildFile()?.name)
  }

  @Test
  fun `findBuildFile null when missing`() {
    val mod = GradleModule(":lib", tempDir.toFile())
    assertNull(mod.findBuildFile())
  }
}

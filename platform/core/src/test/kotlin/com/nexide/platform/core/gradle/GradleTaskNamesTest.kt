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

class GradleTaskNamesTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `root task when no module`() {
    assertEquals("test", GradleTaskNames.forModule(null, null, "test"))
    assertEquals("build", GradleTaskNames.forModulePath(":", "build"))
  }

  @Test
  fun `module scoped task`() {
    val root = tempDir.toFile()
    File(root, "settings.gradle.kts").writeText(
      """
      rootProject.name = "D"
      include(":lib")
      """.trimIndent()
    )
    File(root, "lib").mkdirs()
    File(root, "build.gradle.kts").writeText("//")
    val model = GradleProjectModelLoader.load(root)!!
    val src = File(root, "lib/X.kt")
    src.writeText("class X")
    assertEquals(":lib:test", GradleTaskNames.forModule(model, src, "test"))
  }
}

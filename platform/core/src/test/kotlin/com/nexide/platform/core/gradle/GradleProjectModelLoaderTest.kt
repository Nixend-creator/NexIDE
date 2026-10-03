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

class GradleProjectModelLoaderTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `parses include modules from settings kts`() {
    val root = tempDir.toFile()
    File(root, "settings.gradle.kts").writeText(
      """
      rootProject.name = "Demo"
      include(
        ":platform:core",
        ":ide-app"
      )
      """.trimIndent()
    )
    File(root, "platform/core").mkdirs()
    File(root, "ide-app").mkdirs()
    File(root, "build.gradle.kts").writeText("// root")

    val model = GradleProjectModelLoader.load(root)
    assertNotNull(model)
    assertEquals("Demo", model!!.rootName)
    assertTrue(model.modules.any { it.path == ":platform:core" })
    assertTrue(model.modules.any { it.path == ":ide-app" })

    val file = File(root, "platform/core/src/X.kt")
    file.parentFile.mkdirs()
    file.writeText("class X")
    assertEquals(":platform:core", model.moduleForFile(file)?.path)
  }
}

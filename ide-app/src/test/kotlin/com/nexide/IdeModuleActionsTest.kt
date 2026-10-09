/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide

import com.nexide.platform.core.gradle.GradleModule
import com.nexide.platform.core.gradle.GradleProjectModel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class IdeModuleActionsTest {

  @TempDir
  lateinit var tempDir: Path

  private fun sampleModel(root: File): GradleProjectModel {
    val lib = File(root, "lib").also { it.mkdirs() }
    File(lib, "build.gradle.kts").writeText("//")
    File(root, "src").mkdirs()
    return GradleProjectModel(
      rootDir = root,
      rootName = "Demo",
      modules = listOf(
        GradleModule(":", root),
        GradleModule(":lib", lib)
      )
    )
  }

  @Test
  fun `modulePaths lists all`() {
    val model = sampleModel(tempDir.toFile())
    val paths = IdeModuleActions.modulePaths(model)
    assertTrue(paths.contains(":lib"))
    assertTrue(paths.contains(":"))
  }

  @Test
  fun `moduleForPath picks deepest`() {
    val root = tempDir.toFile()
    val model = sampleModel(root)
    val file = File(root, "lib/X.kt").also { it.writeText("class X") }
    assertEquals(":lib", IdeModuleActions.moduleForPath(model, file.absolutePath))
  }

  @Test
  fun `taskForModulePath and active file`() {
    assertEquals(":lib:test", IdeModuleActions.taskForModulePath(":lib", "test"))
    assertEquals("build", IdeModuleActions.taskForModulePath(":", "build"))
    val root = tempDir.toFile()
    val model = sampleModel(root)
    val file = File(root, "lib/X.kt").also { it.writeText("class X") }
    assertEquals(
      ":lib:build",
      IdeModuleActions.taskForActiveFile(model, file.absolutePath, "build")
    )
  }

  @Test
  fun `paletteActions four per module excluding root`() {
    val model = sampleModel(tempDir.toFile())
    val actions = IdeModuleActions.paletteActions(
      model = model,
      projectBasePath = tempDir.toFile().absolutePath,
      onDismissPalette = {},
      onReveal = {},
      onShowStructure = {},
      onBuildTask = {},
      onTestTask = {},
      onOpenFile = {}
    )
    assertEquals(4, actions.size)
    assertTrue(actions.any { it.title.startsWith("Go to Module") })
    assertTrue(actions.any { it.title.startsWith("Build Module") })
    assertTrue(actions.any { it.title.startsWith("Test Module") })
    assertTrue(actions.any { it.title.startsWith("Open build.gradle") })
  }

  @Test
  fun `revealPaths includes module dir`() {
    val root = tempDir.toFile()
    val lib = File(root, "lib").also { it.mkdirs() }
    val paths = IdeModuleActions.revealPaths(lib, root.absolutePath)
    assertTrue(paths.any { it == lib.absolutePath || it.endsWith("${File.separator}lib") })
  }
}

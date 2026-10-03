/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core.refactor

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class RenameRefactoringTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `skips matches in line comments`() {
    val root = tempDir.toFile()
    File(root, "Sample.kt").writeText(
      """
      fun foo() {}
      // call foo later
      fun bar() { foo() }
      """.trimIndent()
    )
    val preview = RenameRefactoring.preview(root, "foo", "baz")
    assertTrue(preview.occurrences.any { it.lineText.contains("fun foo") || it.line == 1 })
    assertTrue(preview.occurrences.none { it.lineText.trim().startsWith("//") })
  }

  @Test
  fun `apply renames identifier`() {
    val root = tempDir.toFile()
    val f = File(root, "X.kt")
    f.writeText("fun oldName() {}\nfun use() { oldName() }\n")
    val preview = RenameRefactoring.preview(root, "oldName", "newName")
    assertTrue(preview.occurrences.isNotEmpty())
    val n = RenameRefactoring.apply(preview)
    assertTrue(n >= 1)
    val text = f.readText()
    assertTrue(text.contains("newName"))
    assertFalse(text.contains("oldName"))
  }

  @Test
  fun `invalid identifier rejected`() {
    assertFalse(RenameRefactoring.isValidIdentifier("1abc"))
    assertTrue(RenameRefactoring.isValidIdentifier("_ok"))
  }
}

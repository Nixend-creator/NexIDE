/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.io.File

/**
 * Bookmarks write to ~/.nexide — only runs when explicitly enabled
 * to avoid polluting developer home during normal CI.
 * -Dnexide.persist.tests=true
 */
class BookmarksPersistTest {

  @AfterEach
  fun cleanup() {
    Bookmarks.clear()
  }

  @Test
  fun `toggle add and remove in memory`() {
    Bookmarks.clear()
    assertTrue(Bookmarks.toggle("/tmp/a.kt", 3))
    assertTrue(Bookmarks.isBookmarked("/tmp/a.kt", 3))
    assertFalse(Bookmarks.toggle("/tmp/a.kt", 3))
    assertFalse(Bookmarks.isBookmarked("/tmp/a.kt", 3))
  }

  @Test
  fun `next cycles bookmarks`() {
    Bookmarks.clear()
    Bookmarks.toggle("/tmp/a.kt", 1)
    Bookmarks.toggle("/tmp/a.kt", 5)
    val n1 = Bookmarks.next("/tmp/a.kt", 1)
    assertNotNull(n1)
    assertEquals(5, n1!!.line)
    val n2 = Bookmarks.next("/tmp/a.kt", 5)
    assertEquals(1, n2!!.line)
  }
}

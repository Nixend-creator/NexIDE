/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IdeWindowTitleTest {

  @Test
  fun `project only`() {
    assertEquals("NexIDE — Demo", IdeWindowTitle.format("Demo", null, 0, false))
  }

  @Test
  fun `with module and single dirty`() {
    assertEquals(
      "NexIDE — Demo [:lib] •",
      IdeWindowTitle.format("Demo", ":lib", 1, true)
    )
  }

  @Test
  fun `multiple dirty`() {
    assertEquals(
      "NexIDE — Demo •3",
      IdeWindowTitle.format("Demo", ":", 3, false)
    )
  }
}

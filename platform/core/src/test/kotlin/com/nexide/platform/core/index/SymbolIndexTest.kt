/*
 * Copyright 2026 NexIDE Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexide.platform.core.index

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class SymbolIndexTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `indexes kotlin class and function`() {
    val root = tempDir.toFile()
    File(root, "Hello.kt").writeText(
      """
      package demo
      class Hello {
        fun greet() {}
      }
      """.trimIndent()
    )
    SymbolIndex.invalidate()
    SymbolIndex.rebuild(root)

    val hello = SymbolIndex.exact("Hello")
    assertTrue(hello.isNotEmpty())
    assertEquals("class", hello.first().kind)

    val greet = SymbolIndex.exact("greet")
    assertTrue(greet.isNotEmpty())
    assertEquals("fun", greet.first().kind)
  }

  @Test
  fun `lookup by prefix`() {
    val root = tempDir.toFile()
    File(root, "Sample.kt").writeText("class SampleService\nfun sampleHelper() {}")
    SymbolIndex.invalidate()
    SymbolIndex.rebuild(root)

    val hits = SymbolIndex.lookup("Sam")
    assertTrue(hits.any { it.name == "SampleService" })
  }
}

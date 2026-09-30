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

package com.nexide.platform.editor

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AutoPairTest {

  @Test
  fun `pairs parentheses`() {
    val result = AutoPair.apply("", "(", 1)
    assertNotNull(result)
    assertEquals("()", result!!.first)
    assertEquals(1, result.second)
  }

  @Test
  fun `pairs brackets and braces`() {
    assertEquals("[]", AutoPair.apply("", "[", 1)!!.first)
    assertEquals("{}", AutoPair.apply("", "{", 1)!!.first)
  }

  @Test
  fun `pairs double quotes`() {
    val result = AutoPair.apply("", "\"", 1)
    assertNotNull(result)
    assertEquals("\"\"", result!!.first)
    assertEquals(1, result.second)
  }

  @Test
  fun `does not pair apostrophe after letter`() {
    val result = AutoPair.apply("it", "it'", 3)
    assertNull(result)
  }

  @Test
  fun `pairs apostrophe at start of word`() {
    val result = AutoPair.apply("", "'", 1)
    assertNotNull(result)
    assertEquals("''", result!!.first)
  }

  @Test
  fun `skips when closer already next`() {
    val result = AutoPair.apply(")", "()", 1)
    assertNull(result)
  }

  @Test
  fun `ignores multi character edits`() {
    assertNull(AutoPair.apply("a", "abc", 3))
    assertNull(AutoPair.apply("ab", "a", 1))
  }

  @Test
  fun `inserts pair in middle of text`() {
    val result = AutoPair.apply("ab", "a(b", 2)
    assertNotNull(result)
    assertEquals("a()b", result!!.first)
    assertEquals(2, result.second)
  }
}

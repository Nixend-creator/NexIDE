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

class BraceMatcherTest {

  @Test
  fun `matches simple parentheses`() {
    val text = "(x)"
    val pair = BraceMatcher.findPair(text, 0)
    assertNotNull(pair)
    assertEquals(0 to 2, pair)
  }

  @Test
  fun `matches from closing brace`() {
    val text = "{ a }"
    val pair = BraceMatcher.findPair(text, 4)
    assertNotNull(pair)
    assertEquals(0 to 4, pair)
  }

  @Test
  fun `matches nested brackets`() {
    val text = "arr[i[0]]"
    // caret on outer [
    val open = BraceMatcher.findPair(text, 3)
    assertNotNull(open)
    assertEquals(3 to 8, open)
    // caret on outer ]
    val close = BraceMatcher.findPair(text, 8)
    assertNotNull(close)
    assertEquals(3 to 8, close)
  }

  @Test
  fun `matches braces in kotlin-like snippet`() {
    val text = "fun f() { return }"
    val pair = BraceMatcher.findPair(text, 8) // '{'
    assertNotNull(pair)
    assertEquals(8 to 17, pair)
  }

  @Test
  fun `returns null when unmatched`() {
    assertNull(BraceMatcher.findPair("(x", 0))
    assertNull(BraceMatcher.findPair("x)", 1))
  }

  @Test
  fun `returns null for plain text`() {
    assertNull(BraceMatcher.findPair("hello", 2))
  }

  @Test
  fun `uses char before caret when caret after opener`() {
    val text = "()"
    // caret at 1 (between), should still find pair via char before or at
    val pair = BraceMatcher.findPair(text, 1)
    assertNotNull(pair)
    assertEquals(0 to 1, pair)
  }
}

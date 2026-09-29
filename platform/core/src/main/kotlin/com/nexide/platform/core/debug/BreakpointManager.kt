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

package com.nexide.platform.core.debug

/**
 * A line breakpoint in a source file.
 *
 * @param filePath absolute path
 * @param line 1-based line number
 */
data class Breakpoint(
  val filePath: String,
  val line: Int,
  val enabled: Boolean = true
) {
  val key: String get() = "$filePath:$line"
}

/**
 * In-memory breakpoint store (project-scoped in practice via paths).
 */
class BreakpointManager {
  private val myBreakpoints = linkedMapOf<String, Breakpoint>()
  private val myListeners = mutableListOf<() -> Unit>()

  fun getAll(): List<Breakpoint> = myBreakpoints.values.toList()

  fun getForFile(filePath: String): List<Breakpoint> =
    myBreakpoints.values.filter { it.filePath == filePath }.sortedBy { it.line }

  fun hasBreakpoint(filePath: String, line: Int): Boolean =
    myBreakpoints.containsKey(key(filePath, line))

  fun get(filePath: String, line: Int): Breakpoint? =
    myBreakpoints[key(filePath, line)]

  fun toggle(filePath: String, line: Int): Boolean {
    val k = key(filePath, line)
    return if (myBreakpoints.containsKey(k)) {
      myBreakpoints.remove(k)
      fireChanged()
      false
    } else {
      myBreakpoints[k] = Breakpoint(filePath, line)
      fireChanged()
      true
    }
  }

  fun remove(filePath: String, line: Int) {
    if (myBreakpoints.remove(key(filePath, line)) != null) {
      fireChanged()
    }
  }

  fun clearAll() {
    if (myBreakpoints.isNotEmpty()) {
      myBreakpoints.clear()
      fireChanged()
    }
  }

  fun addListener(listener: () -> Unit) {
    myListeners.add(listener)
  }

  fun removeListener(listener: () -> Unit) {
    myListeners.remove(listener)
  }

  private fun fireChanged() {
    myListeners.toList().forEach { it.invoke() }
  }

  private fun key(filePath: String, line: Int) = "$filePath:$line"

  companion object {
    private var ourInstance: BreakpointManager? = null

    fun getInstance(): BreakpointManager {
      if (ourInstance == null) {
        ourInstance = BreakpointManager()
      }
      return ourInstance!!
    }
  }
}

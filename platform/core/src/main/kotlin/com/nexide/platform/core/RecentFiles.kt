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

package com.nexide.platform.core

import java.util.LinkedList

/**
 * In-session list of recently opened files (MRU).
 */
object RecentFiles {
  private const val MAX = 30
  private val paths = LinkedList<String>()

  fun add(path: String) {
    if (path.isBlank()) return
    paths.remove(path)
    paths.addFirst(path)
    while (paths.size > MAX) paths.removeLast()
  }

  fun list(): List<String> = paths.toList()

  fun clear() {
    paths.clear()
  }
}

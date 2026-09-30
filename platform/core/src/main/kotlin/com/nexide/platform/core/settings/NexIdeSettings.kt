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

package com.nexide.platform.core.settings

import com.nexide.platform.util.Logger
import java.io.File
import java.util.Properties

/**
 * Simple persistent IDE settings stored under ~/.nexide/settings.properties.
 */
object NexIdeSettings {
  private val storeFile: File by lazy {
    File(System.getProperty("user.home"), ".nexide/settings.properties").also { f ->
      f.parentFile?.mkdirs()
    }
  }

  private val props = Properties()
  private var loaded = false

  private fun ensureLoaded() {
    if (loaded) return
    loaded = true
    try {
      if (storeFile.exists()) {
        storeFile.inputStream().use { props.load(it) }
      }
    } catch (e: Exception) {
      Logger.warn("Failed to load settings: ${e.message}")
    }
  }

  fun save() {
    ensureLoaded()
    try {
      storeFile.parentFile?.mkdirs()
      storeFile.outputStream().use { props.store(it, "NexIDE settings") }
    } catch (e: Exception) {
      Logger.warn("Failed to save settings: ${e.message}")
    }
  }

  var editorFontSize: Int
    get() {
      ensureLoaded()
      return props.getProperty("editor.fontSize", "14").toIntOrNull()?.coerceIn(10, 32) ?: 14
    }
    set(value) {
      ensureLoaded()
      props.setProperty("editor.fontSize", value.coerceIn(10, 32).toString())
    }

  var showLineNumbers: Boolean
    get() {
      ensureLoaded()
      return props.getProperty("editor.showLineNumbers", "true").toBoolean()
    }
    set(value) {
      ensureLoaded()
      props.setProperty("editor.showLineNumbers", value.toString())
    }

  var tabSize: Int
    get() {
      ensureLoaded()
      return props.getProperty("editor.tabSize", "2").toIntOrNull()?.coerceIn(1, 8) ?: 2
    }
    set(value) {
      ensureLoaded()
      props.setProperty("editor.tabSize", value.coerceIn(1, 8).toString())
    }

  var softWrap: Boolean
    get() {
      ensureLoaded()
      return props.getProperty("editor.softWrap", "false").toBoolean()
    }
    set(value) {
      ensureLoaded()
      props.setProperty("editor.softWrap", value.toString())
    }
}

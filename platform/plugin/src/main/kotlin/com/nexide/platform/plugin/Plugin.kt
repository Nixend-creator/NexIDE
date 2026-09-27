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

package com.nexide.platform.plugin

/**
 * Represents a loaded plugin.
 * Analogous to com.intellij.ide.plugins.IdeaPluginDescriptor (simplified).
 */
interface Plugin {
  /** Unique plugin id, e.g. "com.nexide.lang.kotlin". */
  val pluginId: String

  /** Human-readable name. */
  val name: String

  /** Plugin version. */
  val version: String

  /**
   * Called when the plugin is loaded.
   * Register extension points and extensions here.
   */
  fun initialize()

  /**
   * Called when the plugin is unloaded (optional).
   */
  fun dispose() {}
}

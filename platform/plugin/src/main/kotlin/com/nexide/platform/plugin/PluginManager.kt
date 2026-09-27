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

import com.nexide.platform.util.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages loading and lifecycle of plugins.
 * Analogous to com.intellij.ide.plugins.PluginManagerCore (simplified).
 */
class PluginManager private constructor() {

  private val myPlugins = ConcurrentHashMap<String, Plugin>()

  /**
   * Registers and initializes a plugin.
   */
  fun loadPlugin(plugin: Plugin) {
    if (myPlugins.containsKey(plugin.pluginId)) {
      Logger.warn("Plugin already loaded: ${plugin.pluginId}")
      return
    }

    try {
      plugin.initialize()
      myPlugins[plugin.pluginId] = plugin
      Logger.info("Loaded plugin: ${plugin.name} (${plugin.pluginId}) v${plugin.version}")
    } catch (e: Exception) {
      Logger.error("Failed to load plugin: ${plugin.pluginId}", e)
    }
  }

  /**
   * Returns a loaded plugin by id, or null.
   */
  fun findPlugin(pluginId: String): Plugin? = myPlugins[pluginId]

  /**
   * Returns all loaded plugins.
   */
  fun getLoadedPlugins(): List<Plugin> = myPlugins.values.toList()

  /**
   * Unloads a plugin.
   */
  fun unloadPlugin(pluginId: String) {
    val plugin = myPlugins.remove(pluginId) ?: return
    try {
      plugin.dispose()
      Logger.info("Unloaded plugin: ${plugin.pluginId}")
    } catch (e: Exception) {
      Logger.error("Error while unloading plugin: ${plugin.pluginId}", e)
    }
  }

  companion object {
    private val ourInstance = PluginManager()

    fun getInstance(): PluginManager = ourInstance
  }
}

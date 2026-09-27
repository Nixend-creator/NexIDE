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

import com.nexide.platform.plugin.example.CoreFileTypesPlugin
import com.nexide.platform.plugin.example.FILE_TYPE_EP
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PluginSystemTest {

  @AfterEach
  fun cleanup() {
    Extensions.clearAll()
  }

  @Test
  fun `extension point registers and returns extensions`() {
    val ep = ExtensionPointName<String>("test.ep")
    Extensions.registerExtension(ep, "one")
    Extensions.registerExtension(ep, "two")

    assertEquals(listOf("one", "two"), ep.extensionList)
    assertEquals("one", ep.extension)
  }

  @Test
  fun `core file types plugin registers file types`() {
    val pluginManager = PluginManager.getInstance()
    pluginManager.loadPlugin(CoreFileTypesPlugin())

    val fileTypes = FILE_TYPE_EP.extensionList
    assertTrue(fileTypes.any { it.name == "Kotlin" && it.defaultExtension == "kt" })
    assertTrue(fileTypes.any { it.name == "Java" })
    assertTrue(fileTypes.any { it.name == "Text" })
  }

  @Test
  fun `plugin manager tracks loaded plugins`() {
    val pluginManager = PluginManager.getInstance()
    val plugin = CoreFileTypesPlugin()
    pluginManager.loadPlugin(plugin)

    assertNotNull(pluginManager.findPlugin("com.nexide.core.fileTypes"))
    assertTrue(pluginManager.getLoadedPlugins().any { it.pluginId == plugin.pluginId })
  }
}

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

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Central registry of extension points and their extensions.
 * Analogous to com.intellij.openapi.extensions.Extensions (simplified).
 */
object Extensions {

  private val myExtensionPoints = ConcurrentHashMap<String, ExtensionPoint<*>>()

  /**
   * Registers an extension for the given extension point.
   * Thread-safe.
   */
  fun <T : Any> registerExtension(epName: ExtensionPointName<T>, extension: T) {
    @Suppress("UNCHECKED_CAST")
    val ep = myExtensionPoints.getOrPut(epName.name) {
      ExtensionPoint<T>(epName)
    } as ExtensionPoint<T>
    ep.registerExtension(extension)
  }

  /**
   * Returns all extensions registered for the given point.
   */
  fun <T : Any> getExtensions(epName: ExtensionPointName<T>): List<T> {
    @Suppress("UNCHECKED_CAST")
    val ep = myExtensionPoints[epName.name] as? ExtensionPoint<T>
    return ep?.extensions ?: emptyList()
  }

  /**
   * Clears all registered extensions (mainly for tests).
   */
  fun clearAll() {
    myExtensionPoints.clear()
  }
}

/**
 * Holds extensions for a single extension point.
 */
internal class ExtensionPoint<T : Any>(
  val name: ExtensionPointName<T>
) {
  private val myExtensions = CopyOnWriteArrayList<T>()

  val extensions: List<T>
    get() = myExtensions.toList()

  fun registerExtension(extension: T) {
    myExtensions.add(extension)
  }
}

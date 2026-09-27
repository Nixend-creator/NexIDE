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
 * Strongly-typed name of an extension point.
 * Analogous to com.intellij.openapi.extensions.ExtensionPointName.
 *
 * Usage:
 * ```
 * val EP_NAME = ExtensionPointName<MyExtension>("com.nexide.myExtension")
 * val extensions = EP_NAME.extensionList
 * ```
 */
class ExtensionPointName<T : Any>(val name: String) {

  /**
   * Returns all registered extensions for this point.
   */
  val extensionList: List<T>
    get() = Extensions.getExtensions(this)

  /**
   * Returns the first extension or null.
   */
  val extension: T?
    get() = extensionList.firstOrNull()

  override fun toString(): String = "EP:$name"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ExtensionPointName<*>) return false
    return name == other.name
  }

  override fun hashCode(): Int = name.hashCode()
}

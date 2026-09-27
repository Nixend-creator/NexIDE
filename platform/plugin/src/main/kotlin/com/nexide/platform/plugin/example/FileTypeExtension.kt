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

package com.nexide.platform.plugin.example

import com.nexide.platform.plugin.ExtensionPointName
import com.nexide.platform.plugin.Extensions
import com.nexide.platform.plugin.Plugin

/**
 * Example extension point: contributes a file type.
 * Later will be used by VFS / Editor / PSI.
 */
interface FileType {
  val name: String
  val defaultExtension: String
  val language: String
}

/**
 * Extension point name for file types.
 */
val FILE_TYPE_EP = ExtensionPointName<FileType>("com.nexide.fileType")

/**
 * Built-in plugin that registers a few default file types.
 */
class CoreFileTypesPlugin : Plugin {
  override val pluginId: String = "com.nexide.core.fileTypes"
  override val name: String = "Core File Types"
  override val version: String = "0.1.0"

  override fun initialize() {
    Extensions.registerExtension(FILE_TYPE_EP, object : FileType {
      override val name = "Kotlin"
      override val defaultExtension = "kt"
      override val language = "kotlin"
    })
    Extensions.registerExtension(FILE_TYPE_EP, object : FileType {
      override val name = "Java"
      override val defaultExtension = "java"
      override val language = "java"
    })
    Extensions.registerExtension(FILE_TYPE_EP, object : FileType {
      override val name = "Text"
      override val defaultExtension = "txt"
      override val language = "TEXT"
    })
  }
}

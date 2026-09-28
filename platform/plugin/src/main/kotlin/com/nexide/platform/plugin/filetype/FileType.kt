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

package com.nexide.platform.plugin.filetype

import com.nexide.platform.plugin.ExtensionPointName
import com.nexide.platform.plugin.Extensions

/**
 * Describes a file type contributed by a plugin.
 */
interface FileType {
  val name: String
  val defaultExtension: String
  val language: String
}

val FILE_TYPE_EP = ExtensionPointName<FileType>("com.nexide.fileType")

/**
 * Resolves [FileType] by file name / extension using registered extensions.
 */
object FileTypeRegistry {
  fun getFileTypeByFileName(fileName: String): FileType? {
    val ext = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    if (ext.isEmpty()) return null
    return FILE_TYPE_EP.extensionList.firstOrNull { it.defaultExtension.equals(ext, ignoreCase = true) }
  }

  fun getLanguageByFileName(fileName: String): String {
    return getFileTypeByFileName(fileName)?.language ?: "TEXT"
  }
}

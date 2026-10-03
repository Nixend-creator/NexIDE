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

package com.nexide.platform.core.gradle

import java.io.File

/**
 * Resolves root vs module-scoped Gradle task names.
 */
object GradleTaskNames {

  /**
   * @param suffix e.g. "build", "test"
   * @return "test" or ":platform:core:test"
   */
  fun forModule(model: GradleProjectModel?, file: File?, suffix: String): String {
    if (file == null || model == null) return suffix
    val mod = model.moduleForFile(file) ?: return suffix
    if (mod.path == ":" || mod.path.isBlank()) return suffix
    return "${mod.path}:$suffix"
  }

  fun forModulePath(modulePath: String?, suffix: String): String {
    if (modulePath.isNullOrBlank() || modulePath == ":") return suffix
    return "$modulePath:$suffix"
  }
}

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

import com.nexide.platform.core.run.GradleRunSupport
import java.io.File

data class GradleModule(
  val path: String, // ":platform:core"
  val dir: File
) {
  val name: String get() = path.substringAfterLast(':').ifBlank { path }

  fun findBuildFile(): File? =
    listOf("build.gradle.kts", "build.gradle")
      .map { File(dir, it) }
      .firstOrNull { it.isFile }
}

data class GradleProjectModel(
  val rootDir: File,
  val rootName: String,
  val modules: List<GradleModule>
) {
  fun moduleForFile(file: File): GradleModule? {
    val abs = try {
      file.canonicalFile
    } catch (_: Exception) {
      file.absoluteFile
    }
    return modules
      .filter { mod ->
        val base = try {
          mod.dir.canonicalFile
        } catch (_: Exception) {
          mod.dir.absoluteFile
        }
        abs.path.startsWith(base.path + File.separator) || abs.path == base.path
      }
      .maxByOrNull { it.dir.path.length }
  }
}

/**
 * Lightweight Gradle multi-module model from settings.gradle(.kts) text.
 * Not the Tooling API — good enough for navigation and module-aware UI.
 */
object GradleProjectModelLoader {

  private val ROOT_NAME = Regex("""rootProject\.name\s*=\s*["']([^"']+)["']""")
  // include(":a", ":b") or include ':a', ':b' or include("a")
  private val INCLUDE_STRING = Regex("""["'](:?[\w.:-]+)["']""")

  fun load(projectDir: File): GradleProjectModel? {
    if (!GradleRunSupport.isGradleProject(projectDir)) return null
    val settings = listOf("settings.gradle.kts", "settings.gradle")
      .map { File(projectDir, it) }
      .firstOrNull { it.isFile }
      ?: return GradleProjectModel(projectDir, projectDir.name, listOf(GradleModule(":", projectDir)))

    val text = try {
      settings.readText()
    } catch (_: Exception) {
      return null
    }
    val rootName = ROOT_NAME.find(text)?.groupValues?.get(1) ?: projectDir.name
    val paths = linkedSetOf<String>()
    // Only scan include(...) blocks roughly: lines containing include
    text.lineSequence().forEach { line ->
      if (!line.contains("include")) return@forEach
      INCLUDE_STRING.findAll(line).forEach { m ->
        var p = m.groupValues[1]
        if (!p.startsWith(":")) p = ":$p"
        if (p != ":" && p.length > 1) paths.add(p)
      }
    }
    val modules = mutableListOf(GradleModule(":", projectDir))
    paths.forEach { path ->
      val rel = path.trimStart(':').replace(':', File.separatorChar)
      val dir = File(projectDir, rel)
      if (dir.isDirectory) {
        modules.add(GradleModule(path, dir))
      }
    }
    return GradleProjectModel(projectDir, rootName, modules.distinctBy { it.path })
  }
}

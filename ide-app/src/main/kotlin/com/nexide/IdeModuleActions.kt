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

package com.nexide

import com.nexide.platform.core.gradle.GradleModule
import com.nexide.platform.core.gradle.GradleProjectModel
import com.nexide.platform.core.gradle.GradleTaskNames
import com.nexide.platform.core.notify.NotificationService
import com.nexide.platform.core.vfs.LocalFileSystem
import com.nexide.platform.core.vfs.VirtualFile
import com.nexide.platform.ui.navigation.PaletteAction
import java.io.File

/**
 * Module-scoped actions shared by Command Palette, Structure, and menus.
 */
object IdeModuleActions {

  /** settings / build scripts / properties / version catalog — trigger model refresh on save. */
  fun isGradleMetaFile(file: File): Boolean {
    val n = file.name
    return when {
      n == "settings.gradle" || n == "settings.gradle.kts" -> true
      n == "build.gradle" || n == "build.gradle.kts" -> true
      n == "gradle.properties" -> true
      n == "libs.versions.toml" && file.parentFile?.name == "gradle" -> true
      else -> false
    }
  }

  /** settings or build scripts also warrant symbol reindex. */
  fun shouldReindexAfterGradleSave(file: File): Boolean {
    val n = file.name
    return n.startsWith("settings.gradle") || n.startsWith("build.gradle")
  }

  fun modulePaths(model: GradleProjectModel?): List<String> =
    model?.modules?.map { it.path } ?: emptyList()

  fun moduleForPath(model: GradleProjectModel?, path: String): String? =
    model?.moduleForFile(File(path))?.path

  fun taskForActiveFile(model: GradleProjectModel?, filePath: String?, suffix: String): String =
    GradleTaskNames.forModule(
      model,
      filePath?.let { File(it) },
      suffix
    )

  fun taskForModulePath(modulePath: String, suffix: String): String =
    GradleTaskNames.forModulePath(modulePath, suffix)

  /**
   * Expand module folder in Project View; returns absolute dir path.
   */
  fun revealPaths(moduleDir: File, projectBasePath: String): Set<String> {
    val abs = moduleDir.absolutePath
    return IdeNavigationService.ancestorPaths(abs, projectBasePath) + abs
  }

  fun openBuildFile(module: GradleModule): VirtualFile? {
    val buildFile = module.findBuildFile() ?: return null
    return LocalFileSystem.getInstance().findFileByPath(buildFile.absolutePath)
  }

  fun openBuildFileByPath(model: GradleProjectModel?, modulePath: String): VirtualFile? {
    val mod = model?.modules?.firstOrNull { it.path == modulePath } ?: return null
    return openBuildFile(mod)
  }

  fun openBuildFileByPath(
    projectBasePath: String,
    modulePath: String
  ): VirtualFile? {
    val rel = if (modulePath == ":") "." else modulePath.trimStart(':').replace(':', '/')
    val dir = File(projectBasePath, rel)
    val buildFile = listOf("build.gradle.kts", "build.gradle")
      .map { File(dir, it) }
      .firstOrNull { it.isFile }
      ?: return null
    return LocalFileSystem.getInstance().findFileByPath(buildFile.absolutePath)
  }

  fun openRootBuildGradle(projectBasePath: String): VirtualFile? {
    val dir = File(projectBasePath)
    val build = listOf("build.gradle.kts", "build.gradle")
      .map { File(dir, it) }
      .firstOrNull { it.isFile }
      ?: return null
    return LocalFileSystem.getInstance().findFileByPath(build.absolutePath)
  }

  fun openProjectFile(projectBasePath: String, relative: String): VirtualFile? {
    val f = File(projectBasePath, relative)
    if (!f.isFile) return null
    return LocalFileSystem.getInstance().findFileByPath(f.absolutePath)
  }

  fun openGradleProperties(projectBasePath: String): VirtualFile? =
    openProjectFile(projectBasePath, "gradle.properties")

  fun openVersionCatalog(projectBasePath: String): VirtualFile? =
    openProjectFile(projectBasePath, "gradle/libs.versions.toml")
      ?: openProjectFile(projectBasePath, "gradle/libs.versions.toml".replace("/", File.separator))

  fun openSettingsGradle(projectBasePath: String): VirtualFile? {
    val dir = File(projectBasePath)
    val settings = listOf("settings.gradle.kts", "settings.gradle")
      .map { File(dir, it) }
      .firstOrNull { it.isFile }
      ?: return null
    return LocalFileSystem.getInstance().findFileByPath(settings.absolutePath)
  }

  /**
   * Palette entries: Go to / Build / Test / Open build.gradle per non-root module.
   */
  fun paletteActions(
    model: GradleProjectModel?,
    projectBasePath: String,
    onDismissPalette: () -> Unit,
    onReveal: (expandPaths: Set<String>) -> Unit,
    onShowStructure: () -> Unit,
    onBuildTask: (String) -> Unit,
    onTestTask: (String) -> Unit,
    onOpenFile: (VirtualFile) -> Unit
  ): List<PaletteAction> {
    val mods = model?.modules?.filter { it.path != ":" } ?: return emptyList()
    val perModule = mods.flatMap { mod ->
      listOf(
        PaletteAction(
          id = "module.goto.${mod.path}",
          title = "Go to Module ${mod.path}",
          run = {
            onDismissPalette()
            onReveal(revealPaths(mod.dir, projectBasePath))
            onShowStructure()
            NotificationService.info("Module", mod.path)
          }
        ),
        PaletteAction(
          id = "module.build.${mod.path}",
          title = "Build Module ${mod.path}",
          run = {
            onDismissPalette()
            val task = taskForModulePath(mod.path, "build")
            NotificationService.info("Build", task)
            onBuildTask(task)
          }
        ),
        PaletteAction(
          id = "module.test.${mod.path}",
          title = "Test Module ${mod.path}",
          run = {
            onDismissPalette()
            val task = taskForModulePath(mod.path, "test")
            NotificationService.info("Tests", task)
            onTestTask(task)
          }
        ),
        PaletteAction(
          id = "module.buildfile.${mod.path}",
          title = "Open build.gradle ${mod.path}",
          run = {
            onDismissPalette()
            val vf = openBuildFile(mod)
            if (vf != null) onOpenFile(vf)
            else NotificationService.warn("Module", "No build.gradle in ${mod.path}")
          }
        )
      )
    }
    return perModule
  }
}

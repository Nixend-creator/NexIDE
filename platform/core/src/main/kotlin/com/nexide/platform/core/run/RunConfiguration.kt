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

package com.nexide.platform.core.run

import com.nexide.platform.util.Logger
import java.io.File

enum class RunConfigurationType {
  GRADLE,
  APPLICATION
}

/**
 * Named run/debug configuration.
 */
data class RunConfiguration(
  val name: String,
  val type: RunConfigurationType,
  /** Gradle task name, e.g. "build", "test", ":ide-app:run" */
  val gradleTask: String = "",
  /** Fully-qualified main class for APPLICATION type */
  val mainClass: String = "",
  /** Working directory (usually project root) */
  val workingDir: String = "",
  val debug: Boolean = false
)

/**
 * In-memory store of run configurations for the current IDE session.
 * Seeds defaults for Gradle projects.
 */
class RunConfigurationManager {

  private val myConfigs = mutableListOf<RunConfiguration>()
  private var mySelectedName: String? = null

  val configurations: List<RunConfiguration>
    get() = myConfigs.toList()

  var selected: RunConfiguration?
    get() = myConfigs.firstOrNull { it.name == mySelectedName } ?: myConfigs.firstOrNull()
    set(value) {
      mySelectedName = value?.name
    }

  fun add(config: RunConfiguration) {
    myConfigs.removeAll { it.name == config.name }
    myConfigs.add(config)
    if (mySelectedName == null) {
      mySelectedName = config.name
    }
  }

  fun remove(name: String) {
    myConfigs.removeAll { it.name == name }
    if (mySelectedName == name) {
      mySelectedName = myConfigs.firstOrNull()?.name
    }
  }

  fun clear() {
    myConfigs.clear()
    mySelectedName = null
  }

  /**
   * Seeds common Gradle configs if the project looks like Gradle and list is empty.
   */
  fun ensureDefaults(projectDir: File) {
    if (myConfigs.isNotEmpty()) return
    if (!GradleRunSupport.isGradleProject(projectDir)) return

    add(RunConfiguration("Gradle build", RunConfigurationType.GRADLE, gradleTask = "build", workingDir = projectDir.absolutePath))
    add(RunConfiguration("Gradle test", RunConfigurationType.GRADLE, gradleTask = "test", workingDir = projectDir.absolutePath))
    add(RunConfiguration("Gradle run", RunConfigurationType.GRADLE, gradleTask = "run", workingDir = projectDir.absolutePath))
    // ide-app module run is common for this repo
    if (File(projectDir, "ide-app").isDirectory) {
      add(
        RunConfiguration(
          "NexIDE app",
          RunConfigurationType.GRADLE,
          gradleTask = ":ide-app:run",
          workingDir = projectDir.absolutePath
        )
      )
    }
    mySelectedName = myConfigs.firstOrNull()?.name
  }

  /**
   * Builds the process command for [config]. Returns null if not runnable.
   */
  fun resolveCommand(config: RunConfiguration): List<String>? {
    val dir = File(config.workingDir.ifBlank { "." })
    return when (config.type) {
      RunConfigurationType.GRADLE -> {
        val gradle = GradleRunSupport.findGradleCommand(dir) ?: return null
        val task = config.gradleTask.ifBlank { return null }
        // Support multiple tasks separated by space
        gradle + task.split(Regex("\\s+")).filter { it.isNotEmpty() }
      }
      RunConfigurationType.APPLICATION -> {
        val main = config.mainClass.ifBlank { return null }
        // Minimal: java -cp ... Main — classpath is hard without project model;
        // use Gradle application plugin style as fallback via "run" is preferred.
        // For APPLICATION we try: java with current classpath is unreliable.
        // Launch via gradle if available: run with -PmainClass=
        if (GradleRunSupport.isGradleProject(dir)) {
          val gradle = GradleRunSupport.findGradleCommand(dir) ?: return null
          gradle + listOf("run", "-PmainClass=$main", "--quiet")
        } else {
          listOf("java", main)
        }
      }
    }
  }

  companion object {
    private var ourInstance: RunConfigurationManager? = null

    fun getInstance(): RunConfigurationManager {
      if (ourInstance == null) {
        ourInstance = RunConfigurationManager()
      }
      return ourInstance!!
    }
  }
}

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
    // Discover main classes → APPLICATION configs
    try {
      MainClassDiscovery.discover(projectDir).take(5).forEach { fqcn ->
        val short = fqcn.substringAfterLast('.')
        add(
          RunConfiguration(
            name = "Application: $short",
            type = RunConfigurationType.APPLICATION,
            mainClass = fqcn,
            workingDir = projectDir.absolutePath
          )
        )
      }
    } catch (e: Exception) {
      Logger.warn("Main class discovery failed: ${e.message}")
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
        val cp = ApplicationClasspath.collect(dir)
        if (cp.isNotBlank()) {
          Logger.info("Application classpath entries: ${cp.split(File.pathSeparator).size}")
          listOf("java", "-cp", cp, main)
        } else if (GradleRunSupport.isGradleProject(dir)) {
          // Fallback: Gradle application plugin / run task with main class property
          val gradle = GradleRunSupport.findGradleCommand(dir) ?: return null
          gradle + listOf(
            "run",
            "-PmainClass=$main",
            "-DmainClassName=$main",
            "--args=",
            "--quiet"
          )
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


/**
 * Scans sources for Kotlin/Java main entry points.
 */
object MainClassDiscovery {
  private val KT_MAIN = Regex("""^\s*fun\s+main\s*\(""")
  private val KT_PACKAGE = Regex("""^\s*package\s+([\w.]+)""")
  private val JAVA_MAIN = Regex("""public\s+static\s+void\s+main\s*\(\s*String""")
  private val JAVA_PACKAGE = Regex("""^\s*package\s+([\w.]+)\s*;""")
  private val JAVA_CLASS = Regex("""\b(?:public\s+)?(?:final\s+)?class\s+(\w+)""")

  fun discover(projectRoot: File, limit: Int = 20): List<String> {
    val result = mutableListOf<String>()
    val skip = setOf(".git", ".gradle", "build", "out", "node_modules", ".idea")
    projectRoot.walkTopDown()
      .onEnter { it.name !in skip }
      .filter { it.isFile && it.extension in setOf("kt", "java") }
      .filter { it.length() < 500_000 }
      .forEach { file ->
        if (result.size >= limit) return@forEach
        try {
          val lines = file.readLines(Charsets.UTF_8)
          when (file.extension) {
            "kt" -> {
              if (lines.any { KT_MAIN.containsMatchIn(it) }) {
                val pkg = lines.firstNotNullOfOrNull { KT_PACKAGE.find(it)?.groupValues?.get(1) }
                val simple = file.nameWithoutExtension
                result.add(if (pkg != null) "$pkg.$simple" else simple)
              }
            }
            "java" -> {
              if (lines.any { JAVA_MAIN.containsMatchIn(it) }) {
                val pkg = lines.firstNotNullOfOrNull { JAVA_PACKAGE.find(it)?.groupValues?.get(1) }
                val cls = lines.firstNotNullOfOrNull { JAVA_CLASS.find(it)?.groupValues?.get(1) }
                  ?: file.nameWithoutExtension
                result.add(if (pkg != null) "$pkg.$cls" else cls)
              }
            }
          }
        } catch (_: Exception) {
        }
      }
    return result.distinct()
  }
}

/**
 * Builds a best-effort runtime classpath from Gradle output dirs and jars.
 */
object ApplicationClasspath {
  fun collect(projectDir: File): String {
    val paths = linkedSetOf<String>()
    val skip = setOf(".git", ".gradle", "node_modules", ".idea")
    // Prefer compiled output
    listOf(
      "build/classes/java/main",
      "build/classes/kotlin/main",
      "build/resources/main",
      "out/production/classes",
      "out/production/resources"
    ).forEach { rel ->
      val f = File(projectDir, rel)
      if (f.isDirectory) paths.add(f.absolutePath)
    }
    // Module build dirs (multi-module)
    projectDir.listFiles()?.filter { it.isDirectory && it.name !in skip }?.forEach { mod ->
      listOf(
        "build/classes/java/main",
        "build/classes/kotlin/main",
        "build/resources/main"
      ).forEach { rel ->
        val f = File(mod, rel)
        if (f.isDirectory) paths.add(f.absolutePath)
      }
      val libs = File(mod, "build/libs")
      if (libs.isDirectory) {
        libs.listFiles()?.filter { it.extension == "jar" && !it.name.contains("-sources") }?.forEach {
          paths.add(it.absolutePath)
        }
      }
    }
    val rootLibs = File(projectDir, "build/libs")
    if (rootLibs.isDirectory) {
      rootLibs.listFiles()?.filter { it.extension == "jar" }?.forEach { paths.add(it.absolutePath) }
    }
    val sep = File.pathSeparator
    return paths.joinToString(sep)
  }
}

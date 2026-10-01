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
import java.util.concurrent.TimeUnit

/**
 * Resolves a Gradle project's runtime classpath by injecting a temporary init script
 * and running `nexidePrintClasspath`. Falls back to local build output scan.
 *
 * This is lighter than embedding the full Tooling API while still using Gradle's
 * real dependency resolution.
 */
object GradleClasspathResolver {

  private const val BEGIN = "NEXIDE_CP_BEGIN"
  private const val END = "NEXIDE_CP_END"
  private const val TIMEOUT_SEC = 120L

  /**
   * @return classpath string (pathSeparator-separated) or null if unresolved
   */
  fun resolveRuntimeClasspath(projectDir: File): String? {
    if (!GradleRunSupport.isGradleProject(projectDir)) return null
    val fromGradle = runGradleClasspathTask(projectDir)
    if (!fromGradle.isNullOrBlank()) {
      Logger.info("Gradle classpath entries: ${fromGradle.split(File.pathSeparator).size}")
      return fromGradle
    }
    val local = ApplicationClasspath.collectLocal(projectDir)
    return local.ifBlank { null }
  }

  private fun runGradleClasspathTask(projectDir: File): String? {
    val gradle = GradleRunSupport.findGradleCommand(projectDir) ?: return null
    val initFile = File.createTempFile("nexide-cp-", ".gradle")
    try {
      initFile.writeText(INIT_SCRIPT)
      val cmd = gradle + listOf(
        "-I", initFile.absolutePath,
        "nexidePrintClasspath",
        "--quiet",
        "-q"
      )
      Logger.info("Resolving classpath: ${cmd.joinToString(" ")}")
      val pb = ProcessBuilder(cmd)
        .directory(projectDir)
        .redirectErrorStream(true)
      val process = pb.start()
      val output = process.inputStream.bufferedReader().readText()
      val finished = process.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS)
      if (!finished) {
        process.destroyForcibly()
        Logger.warn("Classpath resolve timed out")
        return null
      }
      return parseClasspathOutput(output)
    } catch (e: Exception) {
      Logger.warn("Classpath resolve failed: ${e.message}")
      return null
    } finally {
      initFile.delete()
    }
  }

  internal fun parseClasspathOutput(output: String): String? {
    val begin = output.indexOf(BEGIN)
    val end = output.indexOf(END)
    if (begin < 0 || end < 0 || end <= begin) return null
    val body = output.substring(begin + BEGIN.length, end).trim()
    return body.ifBlank { null }
  }

  private val INIT_SCRIPT = """
    // Injected by NexIDE — prints runtimeClasspath for the root project
    gradle.projectsLoaded {
      rootProject.allprojects { p ->
        p.afterEvaluate {
          if (p.state.executed || p.state.failure != null) return@afterEvaluate
          p.tasks.register("nexidePrintClasspath") {
            notCompatibleWithConfigurationCache("NexIDE classpath probe")
            doLast {
              def cfg = p.configurations.findByName("runtimeClasspath")
              if (cfg == null) cfg = p.configurations.findByName("runtime")
              if (cfg == null) cfg = p.configurations.findByName("compileClasspath")
              if (cfg != null && cfg.canBeResolved) {
                println("$BEGIN")
                println(cfg.asPath)
                println("$END")
              } else {
                println("$BEGIN")
                println("")
                println("$END")
              }
            }
          }
        }
      }
    }
  """.trimIndent()
}

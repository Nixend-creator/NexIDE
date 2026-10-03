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

import com.nexide.platform.core.build.Problem
import com.nexide.platform.core.build.ProblemParser
import com.nexide.platform.core.build.ProblemSeverity
import com.nexide.platform.core.debug.DebugSessionManager
import com.nexide.platform.core.debug.JdiConnector
import com.nexide.platform.core.index.SymbolIndex
import com.nexide.platform.core.notify.NotificationService
import com.nexide.platform.core.run.GradleClasspathResolver
import com.nexide.platform.core.run.GradleRunSupport
import com.nexide.platform.core.run.ProcessOutputListener
import com.nexide.platform.core.run.ProcessRunner
import com.nexide.platform.core.run.RunConfiguration
import com.nexide.platform.core.run.RunConfigurationManager
import com.nexide.platform.util.Logger
import java.io.File

/**
 * Project session operations extracted from Main (build, test, run, index).
 */
class IdeSessionServices(
  private val processRunner: ProcessRunner,
  private val debugSessionManager: DebugSessionManager,
  private val jdiConnector: JdiConnector,
  private val runConfigManager: RunConfigurationManager
) {

  fun resolveProblemPath(projectDir: File, p: Problem): Problem {
    val path = p.filePath ?: return p
    if (File(path).isAbsolute) return p
    val name = File(path).name
    val match = projectDir.walkTopDown()
      .onEnter { it.name !in setOf(".git", ".gradle", "build", "out", "node_modules") }
      .firstOrNull { it.isFile && it.name == name }
    return if (match != null) p.copy(filePath = match.absolutePath) else p
  }

  fun reindexProjectAsync(projectPath: String) {
    val root = File(projectPath)
    Thread {
      try {
        SymbolIndex.refresh(root)
        Logger.info("SymbolIndex refreshed")
      } catch (e: Exception) {
        Logger.warn("SymbolIndex refresh failed: ${e.message}")
      }
    }.start()
  }

  fun reindexFilesAsync(files: List<File>) {
    if (files.isEmpty()) return
    Thread {
      try {
        files.forEach { SymbolIndex.reindexFile(it) }
        Logger.info("SymbolIndex reindexed ${files.size} file(s)")
      } catch (e: Exception) {
        Logger.warn("SymbolIndex reindexFile failed: ${e.message}")
      }
    }.start()
  }

  /**
   * @param onBuilding true when started, false when finished
   * @param onProblems called with parsed diagnostics
   * @param onShowProblems open Problems panel
   */
  fun buildProject(
    projectPath: String,
    onBuilding: (Boolean) -> Unit,
    onProblems: (List<Problem>) -> Unit,
    onShowProblems: () -> Unit
  ) {
    val dir = File(projectPath)
    if (!GradleRunSupport.isGradleProject(dir)) {
      Logger.warn("Not a Gradle project")
      onShowProblems()
      return
    }
    val gradle = GradleRunSupport.findGradleCommand(dir) ?: return
    onProblems(emptyList())
    onBuilding(true)
    onShowProblems()
    val buffer = StringBuilder()
    val listener = object : ProcessOutputListener {
      override fun onOutput(text: String) {
        buffer.append(text)
      }

      override fun onFinished(exitCode: Int) {
        processRunner.removeListener(this)
        val parsed = ProblemParser.parseOutput(buffer.toString()).map { resolveProblemPath(dir, it) }
        onProblems(parsed)
        onBuilding(false)
        Logger.info("Build finished ($exitCode): ${parsed.size} problem(s)")
        if (parsed.isEmpty()) NotificationService.success("Build OK")
        else NotificationService.warn("Build", "${parsed.size} problem(s)")
        reindexProjectAsync(projectPath)
        GradleClasspathResolver.invalidate(dir)
      }
    }
    processRunner.addListener(listener)
    processRunner.start(gradle + "build", dir)
  }

  fun runTests(
    projectPath: String,
    onBuilding: (Boolean) -> Unit,
    onProblems: (List<Problem>) -> Unit,
    onShowProblems: () -> Unit,
    onShowRun: () -> Unit,
    gradleTask: String = "test"
  ) {
    val dir = File(projectPath)
    if (!GradleRunSupport.isGradleProject(dir)) {
      NotificationService.warn("Tests", "Not a Gradle project")
      return
    }
    val gradle = GradleRunSupport.findGradleCommand(dir) ?: return
    onProblems(emptyList())
    onBuilding(true)
    onShowProblems()
    onShowRun()
    val buffer = StringBuilder()
    val taskLabel = gradleTask
    val listener = object : ProcessOutputListener {
      override fun onOutput(text: String) {
        buffer.append(text)
      }

      override fun onFinished(exitCode: Int) {
        processRunner.removeListener(this)
        val parsed = ProblemParser.parseOutput(buffer.toString()).map { resolveProblemPath(dir, it) }
        onProblems(parsed)
        onBuilding(false)
        val fails = parsed.count { it.severity == ProblemSeverity.ERROR }
        Logger.info("Tests finished ($taskLabel, $exitCode): $fails failure location(s)")
        if (exitCode == 0) NotificationService.success("Tests passed ($taskLabel)")
        else NotificationService.warn("Tests", "$fails failure(s) — $taskLabel")
      }
    }
    processRunner.addListener(listener)
    // gradle list is command + args
    processRunner.start(gradle + taskLabel.split(" ").filter { it.isNotBlank() }, dir)
  }

  fun runConfiguration(
    config: RunConfiguration,
    projectPath: String,
    debug: Boolean,
    onShowRun: () -> Unit,
    onShowDebug: () -> Unit,
    autoAttach: () -> Unit
  ) {
    val dir = File(projectPath)
    val command = runConfigManager.resolveCommand(
      config.copy(workingDir = dir.absolutePath, debug = debug)
    )
    if (command == null) {
      Logger.warn("Cannot resolve run configuration: ${config.name}")
      return
    }
    if (debug) {
      onShowDebug()
      val ok = debugSessionManager.startDebug(command, dir)
      if (ok) autoAttach()
    } else {
      onShowRun()
      processRunner.start(command, dir)
    }
    Logger.info("${if (debug) "Debug" else "Run"}: ${config.name} → ${command.joinToString(" ")}")
  }

  fun stopAll() {
    processRunner.stop()
    debugSessionManager.stop()
    jdiConnector.detach()
  }
}

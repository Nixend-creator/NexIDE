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
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

/**
 * Runs an external process and streams output line by line.
 */
class ProcessRunner {

  private val myProcess = AtomicReference<Process?>(null)
  private val myListeners = ConcurrentLinkedQueue<ProcessOutputListener>()

  val isRunning: Boolean
    get() = myProcess.get()?.isAlive == true

  fun addListener(listener: ProcessOutputListener) {
    myListeners.add(listener)
  }

  fun removeListener(listener: ProcessOutputListener) {
    myListeners.remove(listener)
  }

  /**
   * Starts [command] in [workingDir]. Returns false if already running or failed to start.
   * Optional [environment] entries are merged into the process environment.
   */
  fun start(
    command: List<String>,
    workingDir: File,
    environment: Map<String, String> = emptyMap()
  ): Boolean {
    if (isRunning) {
      Logger.warn("Process already running")
      return false
    }

    return try {
      val builder = ProcessBuilder(command)
        .directory(workingDir)
        .redirectErrorStream(true)
      if (environment.isNotEmpty()) {
        builder.environment().putAll(environment)
      }

      val process = builder.start()
      myProcess.set(process)
      fireStarted(command.joinToString(" "))

      Thread({
        try {
          BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
              fireOutput(line!! + "\n")
            }
          }
          val code = process.waitFor()
          fireFinished(code)
        } catch (e: Exception) {
          fireOutput("[error] ${e.message}\n")
          fireFinished(-1)
        } finally {
          myProcess.set(null)
        }
      }, "nexide-process-runner").apply {
        isDaemon = true
        start()
      }
      true
    } catch (e: Exception) {
      Logger.error("Failed to start process: $command", e)
      fireOutput("[failed to start] ${e.message}\n")
      fireFinished(-1)
      false
    }
  }

  fun stop() {
    val process = myProcess.getAndSet(null) ?: return
    process.destroy()
    // Force kill if still alive after a short moment
    Thread({
      try {
        Thread.sleep(500)
        if (process.isAlive) {
          process.destroyForcibly()
        }
      } catch (_: Exception) {
      }
    }, "nexide-process-stop").apply {
      isDaemon = true
      start()
    }
    fireOutput("\n[process stopped]\n")
    fireFinished(-1)
  }

  private fun fireStarted(command: String) {
    myListeners.forEach { it.onStarted(command) }
  }

  private fun fireOutput(chunk: String) {
    myListeners.forEach { it.onOutput(chunk) }
  }

  private fun fireFinished(exitCode: Int) {
    myListeners.forEach { it.onFinished(exitCode) }
  }
}

interface ProcessOutputListener {
  fun onStarted(command: String) {}
  fun onOutput(text: String)
  fun onFinished(exitCode: Int) {}
}

/**
 * Detects how to invoke Gradle in a project directory.
 */
object GradleRunSupport {
  fun findGradleCommand(projectDir: File): List<String>? {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val wrapper = if (isWindows) {
      File(projectDir, "gradlew.bat")
    } else {
      File(projectDir, "gradlew")
    }
    if (wrapper.exists()) {
      if (!isWindows && !wrapper.canExecute()) {
        wrapper.setExecutable(true)
      }
      return listOf(wrapper.absolutePath)
    }
    // Fallback to system gradle if available
    return listOf("gradle")
  }

  fun isGradleProject(projectDir: File): Boolean {
    return File(projectDir, "build.gradle.kts").exists() ||
      File(projectDir, "build.gradle").exists() ||
      File(projectDir, "settings.gradle.kts").exists() ||
      File(projectDir, "settings.gradle").exists()
  }
}

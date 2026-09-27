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

package com.nexide.platform.core

import com.nexide.platform.util.Logger

/**
 * Central application singleton. Mirrors the role of ApplicationManager
 * in the IntelliJ Platform.
 *
 * Lifecycle:
 * 1. create()
 * 2. start()
 * 3. ... runtime ...
 * 4. exit()
 */
class Application private constructor() {

  private val myProjects = mutableListOf<Project>()
  private var myIsStarted = false

  val isStarted: Boolean
    get() = myIsStarted

  fun start() {
    if (myIsStarted) {
      Logger.warn("Application is already started")
      return
    }
    myIsStarted = true
    Logger.info("NexIDE Application started")
  }

  fun openProject(path: String): Project {
    val project = Project(path)
    myProjects.add(project)
    Logger.info("Opened project: $path")
    return project
  }

  fun getOpenProjects(): List<Project> = myProjects.toList()

  fun exit() {
    if (!myIsStarted) return
    myProjects.clear()
    myIsStarted = false
    Logger.info("NexIDE Application exited")
  }

  companion object {
    @Volatile
    private var ourInstance: Application? = null

    fun getInstance(): Application {
      return ourInstance ?: synchronized(this) {
        ourInstance ?: Application().also { ourInstance = it }
      }
    }

    fun create(): Application {
      return getInstance()
    }
  }
}

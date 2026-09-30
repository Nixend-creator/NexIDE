/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core.state

import com.nexide.platform.util.Logger
import java.io.File
import java.util.Properties

/**
 * Persists IDE session: last project, open tabs, dark theme, soft wrap mirror.
 */
object IdeStateStore {
  private val file: File by lazy {
    File(System.getProperty("user.home"), ".nexide/session.properties").also {
      it.parentFile?.mkdirs()
    }
  }
  private val props = Properties()
  private var loaded = false

  private fun load() {
    if (loaded) return
    loaded = true
    try {
      if (file.exists()) file.inputStream().use { props.load(it) }
    } catch (e: Exception) {
      Logger.warn("IdeStateStore load: ${e.message}")
    }
  }

  fun save() {
    load()
    try {
      file.parentFile?.mkdirs()
      file.outputStream().use { props.store(it, "NexIDE session") }
    } catch (e: Exception) {
      Logger.warn("IdeStateStore save: ${e.message}")
    }
  }

  var lastProjectPath: String?
    get() { load(); return props.getProperty("project.path") }
    set(v) { load(); if (v == null) props.remove("project.path") else props.setProperty("project.path", v) }

  var openTabs: List<String>
    get() {
      load()
      val raw = props.getProperty("tabs", "")
      return if (raw.isBlank()) emptyList() else raw.split('\u0001').filter { it.isNotBlank() }
    }
    set(value) {
      load()
      props.setProperty("tabs", value.joinToString("\u0001"))
    }

  var activeTabPath: String?
    get() { load(); return props.getProperty("tabs.active") }
    set(v) { load(); if (v == null) props.remove("tabs.active") else props.setProperty("tabs.active", v) }

  var darkTheme: Boolean
    get() { load(); return props.getProperty("ui.dark", "true").toBoolean() }
    set(v) { load(); props.setProperty("ui.dark", v.toString()) }

  var splitEditor: Boolean
    get() { load(); return props.getProperty("ui.split", "false").toBoolean() }
    set(v) { load(); props.setProperty("ui.split", v.toString()) }
}

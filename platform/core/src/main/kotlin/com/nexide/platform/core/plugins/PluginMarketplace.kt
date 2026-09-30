/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core.plugins

import java.io.File

data class MarketplacePlugin(
  val id: String,
  val name: String,
  val version: String,
  val description: String,
  val installed: Boolean
)

/**
 * Stub marketplace: built-in catalog + ~/.nexide/plugins directory.
 */
object PluginMarketplace {
  private val catalog = listOf(
    MarketplacePlugin("nexide.theme-extra", "Extra Themes", "0.1.0", "Additional color schemes", false),
    MarketplacePlugin("nexide.icons", "Icon Pack", "0.1.0", "File type icons", false),
    MarketplacePlugin("nexide.keymap-idea", "IntelliJ Keymap", "0.1.0", "Classic IDEA shortcuts overlay", false),
    MarketplacePlugin("nexide.markdown", "Markdown Preview", "0.1.0", "Preview .md files", false)
  )

  fun pluginsDir(): File =
    File(System.getProperty("user.home"), ".nexide/plugins").also { it.mkdirs() }

  fun list(): List<MarketplacePlugin> {
    val installedIds = pluginsDir().listFiles()?.map { it.nameWithoutExtension }?.toSet().orEmpty()
    return catalog.map { it.copy(installed = it.id in installedIds || File(pluginsDir(), it.id).exists()) }
  }

  fun install(id: String): Boolean {
    val plugin = catalog.find { it.id == id } ?: return false
    val marker = File(pluginsDir(), id)
    marker.mkdirs()
    File(marker, "plugin.properties").writeText("id=$id\nname=${plugin.name}\nversion=${plugin.version}\n")
    return true
  }

  fun uninstall(id: String): Boolean {
    val marker = File(pluginsDir(), id)
    return if (marker.exists()) marker.deleteRecursively() else false
  }
}

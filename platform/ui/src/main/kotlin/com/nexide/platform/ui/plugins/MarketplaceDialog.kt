/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.ui.plugins

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nexide.platform.core.plugins.PluginMarketplace

@Composable
fun MarketplaceDialog(onDismiss: () -> Unit) {
  var plugins by remember { mutableStateOf(PluginMarketplace.list()) }
  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
        .padding(16.dp)
        .width(440.dp)
        .heightIn(max = 480.dp)
    ) {
      Text("Plugins", style = MaterialTheme.typography.titleMedium)
      Text(
        "Local catalog + ~/.nexide/plugins (stub marketplace)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp).padding(vertical = 8.dp)) {
        items(plugins, key = { it.id }) { p ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("${p.name}  ${p.version}", style = MaterialTheme.typography.bodyMedium)
              Text(p.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (p.installed) {
              TextButton(onClick = {
                PluginMarketplace.uninstall(p.id)
                plugins = PluginMarketplace.list()
              }) { Text("Remove") }
            } else {
              TextButton(onClick = {
                PluginMarketplace.install(p.id)
                plugins = PluginMarketplace.list()
              }) { Text("Install") }
            }
          }
        }
      }
      TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
    }
  }
}

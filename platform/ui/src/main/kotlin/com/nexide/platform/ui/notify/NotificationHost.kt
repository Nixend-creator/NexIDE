/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.ui.notify

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexide.platform.core.notify.Notification
import com.nexide.platform.core.notify.NotificationService
import com.nexide.platform.core.notify.NotificationType
import javax.swing.SwingUtilities

@Composable
fun NotificationHost(modifier: Modifier = Modifier) {
  var items by remember { mutableStateOf(NotificationService.list()) }
  DisposableEffect(Unit) {
    val listener = {
      SwingUtilities.invokeLater { items = NotificationService.list() }
    }
    NotificationService.addListener(listener)
    onDispose { NotificationService.removeListener(listener) }
  }
  if (items.isEmpty()) return
  Column(
    modifier = modifier
      .widthIn(max = 360.dp)
      .padding(12.dp)
  ) {
    items.take(5).forEach { n ->
      NotificationCard(n) { NotificationService.dismiss(n.id) }
    }
  }
}

@Composable
private fun NotificationCard(n: Notification, onDismiss: () -> Unit) {
  val bg = when (n.type) {
    NotificationType.ERROR -> Color(0xFF5C2B2B)
    NotificationType.WARNING -> Color(0xFF5C4A2B)
    NotificationType.SUCCESS -> Color(0xFF2B5C3A)
    NotificationType.INFO -> Color(0xFF2B3A5C)
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 6.dp)
      .background(bg, MaterialTheme.shapes.small)
      .padding(10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(n.title, style = MaterialTheme.typography.labelLarge, color = Color.White)
      if (n.message.isNotBlank()) {
        Text(n.message, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
      }
    }
    TextButton(onClick = onDismiss) {
      Text("×", color = Color.White)
    }
  }
}

/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide.platform.core.notify

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

enum class NotificationType { INFO, SUCCESS, WARNING, ERROR }

data class Notification(
  val id: Long,
  val type: NotificationType,
  val title: String,
  val message: String,
  val createdAtMs: Long = System.currentTimeMillis()
)

object NotificationService {
  private val seq = AtomicLong(0)
  private val items = CopyOnWriteArrayList<Notification>()
  private val listeners = CopyOnWriteArrayList<() -> Unit>()

  fun list(): List<Notification> = items.toList()

  fun show(type: NotificationType, title: String, message: String = "") {
    items.add(0, Notification(seq.incrementAndGet(), type, title, message))
    while (items.size > 20) items.removeAt(items.lastIndex)
    listeners.forEach { it() }
  }

  fun info(title: String, message: String = "") = show(NotificationType.INFO, title, message)
  fun success(title: String, message: String = "") = show(NotificationType.SUCCESS, title, message)
  fun warn(title: String, message: String = "") = show(NotificationType.WARNING, title, message)
  fun error(title: String, message: String = "") = show(NotificationType.ERROR, title, message)

  fun dismiss(id: Long) {
    items.removeAll { it.id == id }
    listeners.forEach { it() }
  }

  fun clear() {
    items.clear()
    listeners.forEach { it() }
  }

  fun addListener(l: () -> Unit) { listeners.add(l) }
  fun removeListener(l: () -> Unit) { listeners.remove(l) }
}

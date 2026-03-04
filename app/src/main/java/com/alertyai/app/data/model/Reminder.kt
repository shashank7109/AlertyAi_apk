package com.alertyai.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String = "",
    val triggerAt: Long,                       // epoch millis
    val isRepeating: Boolean = false,
    val repeatInterval: RepeatInterval = RepeatInterval.NONE,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class RepeatInterval { NONE, DAILY, WEEKLY, MONTHLY }

package com.alertyai.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val note: String = "",
    val priority: Priority = Priority.NORMAL,
    val isDone: Boolean = false,

    // Date & Time
    val dueDate: Long? = null,         // epoch millis
    val dueTime: Long? = null,         // epoch millis (time portion)

    // Alarm / Reminder
    val alarmEnabled: Boolean = false,
    val remindMinsBefore: Int = 10,    // 0 = at time, 10 = 10 min before, 30, 60, etc.

    // Location
    val location: String = "",

    // Subtasks stored as JSON string
    val subtasksJson: String = "[]",   // List<String> serialized

    // Checklist stored as JSON string
    val checklistJson: String = "[]",  // List<CheckItem> serialized

    val createdAt: Long = System.currentTimeMillis()
)

data class CheckItem(val text: String, val done: Boolean = false)

enum class Priority { LOW, NORMAL, HIGH }


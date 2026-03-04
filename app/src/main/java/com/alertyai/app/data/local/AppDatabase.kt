package com.alertyai.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alertyai.app.data.model.Priority
import com.alertyai.app.data.model.Reminder
import com.alertyai.app.data.model.RepeatInterval
import com.alertyai.app.data.model.Task

@Database(
    entities = [Task::class, Reminder::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "alertyai_db"

        /** Migration from v1 → v2: add new task columns */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN dueTime INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE tasks ADD COLUMN alarmEnabled INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tasks ADD COLUMN remindMinsBefore INTEGER NOT NULL DEFAULT 10")
                database.execSQL("ALTER TABLE tasks ADD COLUMN location TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE tasks ADD COLUMN subtasksJson TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE tasks ADD COLUMN checklistJson TEXT NOT NULL DEFAULT '[]'")
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromPriority(p: Priority): String = p.name
    @TypeConverter
    fun toPriority(s: String): Priority = Priority.valueOf(s)

    @TypeConverter
    fun fromRepeat(r: RepeatInterval): String = r.name
    @TypeConverter
    fun toRepeat(s: String): RepeatInterval = RepeatInterval.valueOf(s)
}

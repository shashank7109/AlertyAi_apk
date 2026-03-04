package com.alertyai.app.data.local

import androidx.room.*
import com.alertyai.app.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY isDone ASC, triggerAt ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isDone = 0 AND triggerAt >= :now ORDER BY triggerAt ASC")
    fun getUpcomingReminders(now: Long = System.currentTimeMillis()): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("UPDATE reminders SET isDone = 1 WHERE id = :id")
    suspend fun markDone(id: Int)
}

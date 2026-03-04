package com.alertyai.app.data.repository

import com.alertyai.app.data.local.ReminderDao
import com.alertyai.app.data.model.Reminder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(private val dao: ReminderDao) {

    fun getAllReminders(): Flow<List<Reminder>> = dao.getAllReminders()
    fun getUpcomingReminders(): Flow<List<Reminder>> = dao.getUpcomingReminders()

    suspend fun addReminder(reminder: Reminder): Long = dao.insertReminder(reminder)
    suspend fun updateReminder(reminder: Reminder) = dao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: Reminder) = dao.deleteReminder(reminder)
    suspend fun markDone(id: Int) = dao.markDone(id)
}

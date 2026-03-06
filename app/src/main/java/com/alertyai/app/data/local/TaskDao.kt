package com.alertyai.app.data.local

import androidx.room.*
import com.alertyai.app.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY isDone ASC, CASE priority WHEN 'HIGH' THEN 0 WHEN 'NORMAL' THEN 1 ELSE 2 END, createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isDone = 0 ORDER BY dueDate ASC, createdAt DESC")
    fun getTodayTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE tasks SET isDone = :done WHERE id = :id")
    suspend fun setTaskDone(id: Int, done: Boolean)

    @Query("DELETE FROM tasks WHERE isDone = 1")
    suspend fun deleteAllCompleted()

    @Query("SELECT title FROM tasks")
    suspend fun getAllTitles(): List<String>

    /** One-shot (non-Flow) query — used for BOOT_COMPLETED alarm re-scheduling. */
    /** One-shot (non-Flow) query — used for syncing. */
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksList(): List<Task>

    /** One-shot (non-Flow) query — used for BOOT_COMPLETED alarm re-scheduling. */
    @Query("SELECT * FROM tasks WHERE alarmEnabled = 1 AND dueDate IS NOT NULL AND dueTime IS NOT NULL AND isDone = 0")
    suspend fun getTasksWithAlarms(): List<Task>
}


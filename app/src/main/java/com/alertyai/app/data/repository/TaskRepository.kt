package com.alertyai.app.data.repository

import com.alertyai.app.data.local.TaskDao
import com.alertyai.app.data.model.Task
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(private val dao: TaskDao) {

    fun getAllTasks(): Flow<List<Task>> = dao.getAllTasks()
    suspend fun getAllTasksList(): List<Task> = dao.getAllTasksList()
    fun getTodayTasks(): Flow<List<Task>> = dao.getTodayTasks()


    suspend fun addTask(task: Task): Long = dao.insertTask(task)
    suspend fun updateTask(task: Task) = dao.updateTask(task)
    suspend fun deleteTask(task: Task) = dao.deleteTask(task)
    suspend fun toggleDone(task: Task) = dao.setTaskDone(task.id, !task.isDone)
    suspend fun deleteCompleted() = dao.deleteAllCompleted()
}

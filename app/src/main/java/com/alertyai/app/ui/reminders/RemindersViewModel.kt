package com.alertyai.app.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertyai.app.data.model.Reminder
import com.alertyai.app.data.model.RepeatInterval
import com.alertyai.app.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val repo: ReminderRepository
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> = repo.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val upcoming: StateFlow<List<Reminder>> = repo.getUpcomingReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addReminder(title: String, body: String, triggerAt: Long, repeat: RepeatInterval) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repo.addReminder(
                Reminder(title = title.trim(), body = body.trim(), triggerAt = triggerAt,
                    isRepeating = repeat != RepeatInterval.NONE, repeatInterval = repeat)
            )
        }
    }

    fun markDone(id: Int) = viewModelScope.launch { repo.markDone(id) }
    fun delete(reminder: Reminder) = viewModelScope.launch { repo.deleteReminder(reminder) }
}

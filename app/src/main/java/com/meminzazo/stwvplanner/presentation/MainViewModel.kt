package com.meminzazo.stwvplanner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meminzazo.stwvplanner.domain.model.User
import com.meminzazo.stwvplanner.domain.repository.AuthRepository
import com.meminzazo.stwvplanner.domain.usecase.ScheduleReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: com.meminzazo.stwvplanner.domain.repository.SyncRepository,
    private val scheduleReminderUseCase: ScheduleReminderUseCase
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        scheduleReminderUseCase()
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    syncRepository.syncAll(user.id)
                }
            }
        }
    }
}

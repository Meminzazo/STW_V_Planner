package com.meminzazo.stwvplanner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meminzazo.stwvplanner.domain.model.User
import com.meminzazo.stwvplanner.domain.repository.AuthRepository
import com.meminzazo.stwvplanner.domain.usecase.ScheduleReminderUseCase
import com.meminzazo.stwvplanner.domain.usecase.ConfigureAutomaticBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val scheduleReminderUseCase: ScheduleReminderUseCase,
    private val configureAutomaticBackupUseCase: ConfigureAutomaticBackupUseCase
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    val isLocalMode: StateFlow<Boolean> = authRepository.isUserLocal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        scheduleReminderUseCase()
        viewModelScope.launch {
            combine(authRepository.currentUser, authRepository.isUserLocal) { user, isLocal ->
                user != null && !isLocal
            }.filter { it }.first()
            configureAutomaticBackupUseCase.runOnAppOpenIfAllowed()
        }
    }
}

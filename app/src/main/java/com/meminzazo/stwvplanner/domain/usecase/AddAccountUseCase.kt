package com.meminzazo.stwvplanner.domain.usecase

import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.repository.VBucksRepository
import javax.inject.Inject

class AddAccountUseCase @Inject constructor(
    private val repository: VBucksRepository
) {
    suspend operator fun invoke(name: String, isMain: Boolean = false, parentAccountId: Long? = null): Long {
        val account = Account(name = name, isMain = isMain, parentAccountId = parentAccountId)
        return repository.insertAccount(account)
    }
}

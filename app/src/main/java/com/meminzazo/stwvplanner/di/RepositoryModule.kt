package com.meminzazo.stwvplanner.di

import com.meminzazo.stwvplanner.data.repository.AuthRepositoryImpl
import com.meminzazo.stwvplanner.data.repository.SyncRepositoryImpl
import com.meminzazo.stwvplanner.data.repository.VBucksRepositoryImpl
import com.meminzazo.stwvplanner.domain.repository.AuthRepository
import com.meminzazo.stwvplanner.domain.repository.SyncRepository
import com.meminzazo.stwvplanner.domain.repository.VBucksRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVBucksRepository(
        impl: VBucksRepositoryImpl
    ): VBucksRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        impl: SyncRepositoryImpl
    ): SyncRepository
}

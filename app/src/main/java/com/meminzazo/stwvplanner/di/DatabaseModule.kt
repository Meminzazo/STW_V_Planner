package com.meminzazo.stwvplanner.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.meminzazo.stwvplanner.data.local.VBucksDatabase
import com.meminzazo.stwvplanner.data.local.dao.AccountDao
import com.meminzazo.stwvplanner.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VBucksDatabase {
        return Room.databaseBuilder(
            context,
            VBucksDatabase::class.java,
            "vbucks_db"
        )
        .addMigrations(MIGRATION_3_4)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideAccountDao(db: VBucksDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideTransactionDao(db: VBucksDatabase): TransactionDao = db.transactionDao()
}

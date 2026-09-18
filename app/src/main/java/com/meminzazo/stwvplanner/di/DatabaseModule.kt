package com.meminzazo.stwvplanner.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.meminzazo.stwvplanner.data.local.VBucksDatabase
import com.meminzazo.stwvplanner.data.local.dao.AccountDao
import com.meminzazo.stwvplanner.data.local.dao.SharedLinkDao
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

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `shared_links` (`code` TEXT NOT NULL, `accountName` TEXT NOT NULL, `ownerName` TEXT, `lastViewed` INTEGER NOT NULL, PRIMARY KEY(`code`))")
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
        .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideAccountDao(db: VBucksDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideTransactionDao(db: VBucksDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideSharedLinkDao(db: VBucksDatabase): SharedLinkDao = db.sharedLinkDao()
}

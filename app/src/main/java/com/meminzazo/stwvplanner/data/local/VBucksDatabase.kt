package com.meminzazo.stwvplanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.meminzazo.stwvplanner.data.local.dao.AccountDao
import com.meminzazo.stwvplanner.data.local.dao.SharedLinkDao
import com.meminzazo.stwvplanner.data.local.dao.TransactionDao
import com.meminzazo.stwvplanner.data.local.entity.AccountEntity
import com.meminzazo.stwvplanner.data.local.entity.SharedLinkEntity
import com.meminzazo.stwvplanner.data.local.entity.TransactionEntity

@Database(entities = [AccountEntity::class, TransactionEntity::class, SharedLinkEntity::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class VBucksDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun sharedLinkDao(): SharedLinkDao
}

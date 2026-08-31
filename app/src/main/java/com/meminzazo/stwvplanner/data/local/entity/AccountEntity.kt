package com.meminzazo.stwvplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentAccountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parentAccountId"])]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val isMain: Boolean = false,
    val parentAccountId: Long? = null,
    val parentSyncId: String? = null,
    val isDeleted: Boolean = false,
    val isSynced: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

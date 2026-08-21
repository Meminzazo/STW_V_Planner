package com.meminzazo.stwvplanner.data.local

import androidx.room.TypeConverter
import com.meminzazo.stwvplanner.domain.model.ItemType
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromVBucksSource(source: VBucksSource): String = source.name

    @TypeConverter
    fun toVBucksSource(value: String): VBucksSource = VBucksSource.valueOf(value)

    @TypeConverter
    fun fromItemType(type: ItemType?): String? = type?.name

    @TypeConverter
    fun toItemType(value: String?): ItemType? = value?.let { ItemType.valueOf(it) }
}

package com.oponexis.companion.data.local

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

@Entity(tableName = "foundation_metadata")
data class FoundationMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Database(entities = [FoundationMetadataEntity::class], version = 1, exportSchema = false)
abstract class SkeletonDatabase : RoomDatabase()

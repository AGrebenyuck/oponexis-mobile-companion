package com.oponexis.companion.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Index
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sms_activity", indices = [Index("provider_message_id")])
data class SmsActivityEntity(
    @PrimaryKey val id: String,
    @androidx.room.ColumnInfo(name = "provider_message_id") val providerMessageId: String?,
    val status: String,
    val source: String,
    @androidx.room.ColumnInfo(name = "phone_number") val phoneNumber: String?,
    val detail: String?,
    @androidx.room.ColumnInfo(name = "occurred_at_epoch_millis") val occurredAtEpochMillis: Long,
)

@Dao
interface SmsActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SmsActivityEntity>)

    @Query("SELECT * FROM sms_activity ORDER BY occurred_at_epoch_millis DESC LIMIT 100")
    fun observeRecent(): Flow<List<SmsActivityEntity>>

    @Query("SELECT * FROM sms_activity ORDER BY occurred_at_epoch_millis DESC LIMIT 100")
    suspend fun recent(): List<SmsActivityEntity>

    @Query(
        "SELECT * FROM sms_activity WHERE provider_message_id = :providerMessageId " +
            "ORDER BY occurred_at_epoch_millis DESC LIMIT 1",
    )
    suspend fun latestForProvider(providerMessageId: String): SmsActivityEntity?

    @Query("DELETE FROM sms_activity WHERE occurred_at_epoch_millis < :cutoffEpochMillis")
    suspend fun deleteBefore(cutoffEpochMillis: Long): Int
}

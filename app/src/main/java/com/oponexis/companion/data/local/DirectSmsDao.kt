package com.oponexis.companion.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "direct_sms_messages", indices = [Index("created_at_epoch_millis"), Index("direction")])
data class DirectSmsMessageEntity(
    @PrimaryKey val id: String,
    val direction: String,
    @ColumnInfo(name = "phone_number") val phoneNumber: String,
    val body: String,
    val status: String,
    val source: String,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "total_parts") val totalParts: Int = 1,
    @ColumnInfo(name = "sent_parts") val sentParts: Int = 0,
    @ColumnInfo(name = "delivered_parts") val deliveredParts: Int = 0,
    val error: String? = null,
)

@Dao
interface DirectSmsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: DirectSmsMessageEntity): Long

    @Query("SELECT * FROM direct_sms_messages ORDER BY created_at_epoch_millis DESC LIMIT 300")
    fun observeAll(): Flow<List<DirectSmsMessageEntity>>

    @Query("SELECT * FROM direct_sms_messages WHERE id = :id LIMIT 1")
    suspend fun find(id: String): DirectSmsMessageEntity?

    @Query("UPDATE direct_sms_messages SET status = 'SENDING', updated_at_epoch_millis = :now WHERE id = :id AND status = 'CLAIMED'")
    suspend fun markSending(id: String, now: Long)

    @Query("UPDATE direct_sms_messages SET sent_parts = sent_parts + 1, status = CASE WHEN sent_parts + 1 >= total_parts THEN 'SENT' ELSE 'SENDING' END, updated_at_epoch_millis = :now WHERE id = :id AND status NOT IN ('FAILED', 'DELIVERED')")
    suspend fun markPartSent(id: String, now: Long)

    @Query("UPDATE direct_sms_messages SET delivered_parts = delivered_parts + 1, status = CASE WHEN delivered_parts + 1 >= total_parts THEN 'DELIVERED' ELSE status END, updated_at_epoch_millis = :now WHERE id = :id AND status != 'FAILED'")
    suspend fun markPartDelivered(id: String, now: Long)

    @Query("UPDATE direct_sms_messages SET status = 'FAILED', error = :error, updated_at_epoch_millis = :now WHERE id = :id AND status != 'DELIVERED'")
    suspend fun markFailed(id: String, error: String, now: Long)
}

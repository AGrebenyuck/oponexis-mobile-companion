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

@Entity(
    tableName = "event_outbox",
    indices = [
        Index(value = ["call_ref"], unique = true),
        Index(value = ["status"]),
    ],
)
data class EventOutboxEntity(
    @PrimaryKey @ColumnInfo(name = "event_id") val eventId: String,
    @ColumnInfo(name = "call_ref") val callRef: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int = 1,
    @ColumnInfo(name = "event_type") val eventType: String = "call_outcome",
    val source: String = "android_post_call",
    val confidence: String = "user_selected",
    @ColumnInfo(name = "observed_at_epoch_millis") val observedAtEpochMillis: Long,
    @ColumnInfo(name = "resolved_at_epoch_millis") val resolvedAtEpochMillis: Long,
    @ColumnInfo(name = "disconnect_category") val disconnectCategory: String,
    @ColumnInfo(name = "duration_bucket") val durationBucket: String,
    @ColumnInfo(name = "outcome_code") val outcomeCode: String,
    @ColumnInfo(name = "topic_label") val topicLabel: String? = null,
    @ColumnInfo(name = "phone_number") val phoneNumber: String?,
    @ColumnInfo(name = "customer_ref") val customerRef: String?,
    val status: String = OutboxStatus.Pending.persistedValue,
    @ColumnInfo(name = "attempt_count") val attemptCount: Int = 0,
    @ColumnInfo(name = "next_attempt_at_epoch_millis") val nextAttemptAtEpochMillis: Long,
    @ColumnInfo(name = "lease_until_epoch_millis") val leaseUntilEpochMillis: Long? = null,
    @ColumnInfo(name = "last_error_category") val lastErrorCategory: String? = null,
    @ColumnInfo(name = "server_receipt_id") val serverReceiptId: String? = null,
    @ColumnInfo(name = "delivered_at_epoch_millis") val deliveredAtEpochMillis: Long? = null,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
)

enum class OutboxStatus(val persistedValue: String) {
    Pending("PENDING"),
    InFlight("IN_FLIGHT"),
    RetryScheduled("RETRY_SCHEDULED"),
    Delivered("DELIVERED"),
    PermanentFailure("PERMANENT_FAILURE"),
}

@Dao
interface EventOutboxDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: EventOutboxEntity)

    @Query("SELECT * FROM event_outbox WHERE event_id = :eventId LIMIT 1")
    suspend fun findByEventId(eventId: String): EventOutboxEntity?

    @Query("SELECT * FROM event_outbox ORDER BY created_at_epoch_millis DESC")
    fun observeAll(): Flow<List<EventOutboxEntity>>

    @Query(
        """
        SELECT * FROM event_outbox
        WHERE status IN ('PENDING', 'RETRY_SCHEDULED')
           OR (status = 'IN_FLIGHT' AND lease_until_epoch_millis <= :nowEpochMillis)
        """,
    )
    suspend fun findSchedulable(nowEpochMillis: Long): List<EventOutboxEntity>

    @Query(
        """
        UPDATE event_outbox
        SET status = 'IN_FLIGHT',
            lease_until_epoch_millis = :leaseUntilEpochMillis,
            last_error_category = NULL
        WHERE event_id = :eventId
          AND (
            status IN ('PENDING', 'RETRY_SCHEDULED')
            OR (status = 'IN_FLIGHT' AND lease_until_epoch_millis <= :nowEpochMillis)
          )
        """,
    )
    suspend fun claim(
        eventId: String,
        nowEpochMillis: Long,
        leaseUntilEpochMillis: Long,
    ): Int

    @Query(
        """
        UPDATE event_outbox
        SET status = 'DELIVERED',
            attempt_count = attempt_count + 1,
            lease_until_epoch_millis = NULL,
            last_error_category = NULL,
            server_receipt_id = :receiptId,
            delivered_at_epoch_millis = :deliveredAtEpochMillis,
            phone_number = NULL,
            customer_ref = NULL
        WHERE event_id = :eventId AND status = 'IN_FLIGHT'
        """,
    )
    suspend fun markDelivered(
        eventId: String,
        receiptId: String,
        deliveredAtEpochMillis: Long,
    ): Int

    @Query(
        """
        UPDATE event_outbox
        SET status = 'RETRY_SCHEDULED',
            attempt_count = attempt_count + 1,
            next_attempt_at_epoch_millis = :nextAttemptAtEpochMillis,
            lease_until_epoch_millis = NULL,
            last_error_category = :errorCategory
        WHERE event_id = :eventId AND status = 'IN_FLIGHT'
        """,
    )
    suspend fun markRetry(
        eventId: String,
        errorCategory: String,
        nextAttemptAtEpochMillis: Long,
    ): Int

    @Query(
        """
        UPDATE event_outbox
        SET status = 'PERMANENT_FAILURE',
            attempt_count = attempt_count + 1,
            lease_until_epoch_millis = NULL,
            last_error_category = :errorCategory
        WHERE event_id = :eventId AND status = 'IN_FLIGHT'
        """,
    )
    suspend fun markPermanentFailure(eventId: String, errorCategory: String): Int

    @Query(
        """
        UPDATE event_outbox
        SET status = 'PENDING',
            next_attempt_at_epoch_millis = :nowEpochMillis,
            lease_until_epoch_millis = NULL,
            last_error_category = NULL
        WHERE event_id = :eventId AND status = 'PERMANENT_FAILURE'
        """,
    )
    suspend fun resetPermanentFailure(eventId: String, nowEpochMillis: Long): Int
}

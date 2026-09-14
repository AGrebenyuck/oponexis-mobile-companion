package com.oponexis.companion.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "call_outcomes")
data class CallOutcomeEntity(
    @PrimaryKey
    @androidx.room.ColumnInfo(name = "call_ref")
    val callRef: String,
    @androidx.room.ColumnInfo(name = "observed_at_epoch_millis")
    val observedAtEpochMillis: Long,
    @androidx.room.ColumnInfo(name = "disconnect_category")
    val disconnectCategory: String,
    @androidx.room.ColumnInfo(name = "duration_bucket")
    val durationBucket: String,
	@androidx.room.ColumnInfo(name = "direction") val direction: String = "INCOMING",
	@androidx.room.ColumnInfo(name = "is_returning_customer") val isReturningCustomer: Boolean = false,
    @androidx.room.ColumnInfo(name = "phone_number")
    val phoneNumber: String? = null,
    @androidx.room.ColumnInfo(name = "display_name")
    val displayName: String? = null,
    @androidx.room.ColumnInfo(name = "history_phone_number")
    val historyPhoneNumber: String? = null,
    @androidx.room.ColumnInfo(name = "history_display_name")
    val historyDisplayName: String? = null,
    @androidx.room.ColumnInfo(name = "customer_ref")
    val customerRef: String? = null,
    @androidx.room.ColumnInfo(name = "outcome_code")
    val outcomeCode: String? = null,
    @androidx.room.ColumnInfo(name = "topic_label")
    val topicLabel: String? = null,
    @androidx.room.ColumnInfo(name = "resolved_at_epoch_millis")
    val resolvedAtEpochMillis: Long? = null,
    @androidx.room.ColumnInfo(name = "dismissed_at_epoch_millis")
    val dismissedAtEpochMillis: Long? = null,
    @androidx.room.ColumnInfo(name = "sms_receipt_id")
    val smsReceiptId: String? = null,
    @androidx.room.ColumnInfo(name = "sms_receipt_type")
    val smsReceiptType: String? = null,
    @androidx.room.ColumnInfo(name = "sms_delivery_status")
    val smsDeliveryStatus: String? = null,
    @androidx.room.ColumnInfo(name = "sms_delivery_detail")
    val smsDeliveryDetail: String? = null,
)

@Dao
interface CallOutcomeDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CallOutcomeEntity)

    @Query(
        """
        SELECT * FROM call_outcomes
        WHERE outcome_code IS NULL AND dismissed_at_epoch_millis IS NULL
        ORDER BY observed_at_epoch_millis ASC
        """,
    )
    fun observePending(): Flow<List<CallOutcomeEntity>>

    @Query("SELECT * FROM call_outcomes ORDER BY observed_at_epoch_millis DESC")
    fun observeHistory(): Flow<List<CallOutcomeEntity>>

    @Query("SELECT COUNT(*) FROM call_outcomes")
    suspend fun countAll(): Int

    @Query("SELECT * FROM call_outcomes WHERE call_ref = :callRef LIMIT 1")
    suspend fun findByCallRef(callRef: String): CallOutcomeEntity?

    @Query(
        """
        SELECT * FROM call_outcomes
        WHERE sms_receipt_id IS NOT NULL
          AND sms_receipt_type IS NOT NULL
          AND COALESCE(sms_delivery_status, 'QUEUED') NOT IN ('SENT', 'DELIVERED', 'FAILED', 'CANCELLED')
        """,
    )
    suspend fun findSmsReceiptsAwaitingStatus(): List<CallOutcomeEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOutbox(entity: EventOutboxEntity)

    @Transaction
    suspend fun selectAndEnqueue(
        callRef: String,
        outcomeCode: String,
        topicLabel: String?,
        eventId: String,
        nowEpochMillis: Long,
    ): Boolean {
        val pending = findByCallRef(callRef)
            ?.takeIf { it.outcomeCode == null && it.dismissedAtEpochMillis == null }
            ?: return false
        if (selectOutcome(callRef, outcomeCode, topicLabel, nowEpochMillis) != 1) return false
        insertOutbox(
            EventOutboxEntity(
                eventId = eventId,
                callRef = pending.callRef,
                observedAtEpochMillis = pending.observedAtEpochMillis,
                resolvedAtEpochMillis = nowEpochMillis,
                disconnectCategory = pending.disconnectCategory,
                durationBucket = pending.durationBucket,
                outcomeCode = outcomeCode,
                topicLabel = topicLabel,
                phoneNumber = pending.phoneNumber,
                customerRef = pending.customerRef,
                nextAttemptAtEpochMillis = nowEpochMillis,
                createdAtEpochMillis = nowEpochMillis,
            ),
        )
        return true
    }

    @Transaction
    suspend fun replaceOutcomeAndEnqueue(
        callRef: String,
        outcomeCode: String,
        topicLabel: String?,
        eventId: String,
        nowEpochMillis: Long,
    ): Boolean {
        val call = findByCallRef(callRef) ?: return false
        val phone = call.phoneNumber ?: call.historyPhoneNumber
        if (replaceOutcome(callRef, outcomeCode, topicLabel, nowEpochMillis) != 1) return false
        deleteOutboxForCall(callRef)
        insertOutbox(
            EventOutboxEntity(
                eventId = eventId,
                callRef = call.callRef,
                observedAtEpochMillis = call.observedAtEpochMillis,
                resolvedAtEpochMillis = nowEpochMillis,
                disconnectCategory = call.disconnectCategory,
                durationBucket = call.durationBucket,
                outcomeCode = outcomeCode,
                topicLabel = topicLabel,
                phoneNumber = phone,
                customerRef = call.customerRef,
                nextAttemptAtEpochMillis = nowEpochMillis,
                createdAtEpochMillis = nowEpochMillis,
            ),
        )
        return true
    }

    @Query(
        """
        UPDATE call_outcomes
        SET outcome_code = :outcomeCode,
            topic_label = :topicLabel,
            resolved_at_epoch_millis = :resolvedAtEpochMillis,
            history_phone_number = phone_number,
            history_display_name = display_name,
            phone_number = NULL,
            display_name = NULL
        WHERE call_ref = :callRef
          AND outcome_code IS NULL
          AND dismissed_at_epoch_millis IS NULL
        """,
    )
    suspend fun selectOutcome(
        callRef: String,
        outcomeCode: String,
        topicLabel: String?,
        resolvedAtEpochMillis: Long,
    ): Int

    @Query(
        """
        UPDATE call_outcomes
        SET outcome_code = :outcomeCode,
            topic_label = :topicLabel,
            resolved_at_epoch_millis = :resolvedAtEpochMillis,
            history_phone_number = COALESCE(history_phone_number, phone_number),
            history_display_name = COALESCE(history_display_name, display_name),
            phone_number = NULL,
            display_name = NULL,
            dismissed_at_epoch_millis = NULL
        WHERE call_ref = :callRef
        """,
    )
    suspend fun replaceOutcome(
        callRef: String,
        outcomeCode: String,
        topicLabel: String?,
        resolvedAtEpochMillis: Long,
    ): Int

    @Query("DELETE FROM event_outbox WHERE call_ref = :callRef")
    suspend fun deleteOutboxForCall(callRef: String): Int

    @Query(
        """
        UPDATE call_outcomes
        SET dismissed_at_epoch_millis = :dismissedAtEpochMillis,
            history_phone_number = phone_number,
            history_display_name = display_name,
            phone_number = NULL,
            display_name = NULL
        WHERE call_ref = :callRef
          AND outcome_code IS NULL
          AND dismissed_at_epoch_millis IS NULL
        """,
    )
    suspend fun dismiss(callRef: String, dismissedAtEpochMillis: Long): Int

    @Query(
        """
        UPDATE call_outcomes
        SET display_name = COALESCE(:displayName, display_name),
            history_display_name = COALESCE(:displayName, history_display_name),
            customer_ref = :customerRef,
            is_returning_customer = :isReturningCustomer
        WHERE call_ref = :callRef
        """,
    )
    suspend fun attachIdentity(
        callRef: String,
        displayName: String?,
        customerRef: String,
        isReturningCustomer: Boolean,
    ): Int

    @Query(
        """
        UPDATE call_outcomes
        SET display_name = COALESCE(:displayName, display_name),
            history_display_name = COALESCE(:displayName, history_display_name),
            customer_ref = :customerRef,
            is_returning_customer = :isReturningCustomer
        WHERE phone_number = :phoneNumber OR history_phone_number = :phoneNumber
        """,
    )
    suspend fun attachIdentityForPhone(
        phoneNumber: String,
        displayName: String?,
        customerRef: String,
        isReturningCustomer: Boolean,
    ): Int

    @Query(
        """
        UPDATE call_outcomes
        SET display_name = NULL,
            history_display_name = NULL,
            customer_ref = NULL,
            is_returning_customer = 0
        WHERE phone_number = :phoneNumber OR history_phone_number = :phoneNumber
        """,
    )
    suspend fun clearIdentityForPhone(phoneNumber: String): Int

    @Query(
        """
        UPDATE call_outcomes
        SET sms_receipt_id = :receiptId,
            sms_receipt_type = :receiptType,
            sms_delivery_status = :status,
            sms_delivery_detail = NULL
        WHERE call_ref = :callRef
        """,
    )
    suspend fun recordSmsReceipt(callRef: String, receiptId: String, receiptType: String, status: String): Int

    @Query(
        """
        UPDATE call_outcomes
        SET sms_delivery_status = :status,
            sms_delivery_detail = :detail
        WHERE call_ref = :callRef
        """,
    )
    suspend fun updateSmsDeliveryStatus(callRef: String, status: String, detail: String?): Int

    @Query(
        """
        UPDATE call_outcomes
        SET sms_delivery_status = :status,
            sms_delivery_detail = :detail
        WHERE sms_receipt_id = :providerMessageId
        """,
    )
    suspend fun updateSmsDeliveryStatusByReceipt(
        providerMessageId: String,
        status: String,
        detail: String?,
    ): Int

    @Query(
        """
        DELETE FROM call_outcomes
        WHERE COALESCE(resolved_at_epoch_millis, dismissed_at_epoch_millis) < :cutoffEpochMillis
          AND (outcome_code IS NOT NULL OR dismissed_at_epoch_millis IS NOT NULL)
          AND NOT EXISTS (
              SELECT 1 FROM event_outbox
              WHERE event_outbox.call_ref = call_outcomes.call_ref
                AND event_outbox.status != 'DELIVERED'
          )
        """,
    )
    suspend fun deleteResolvedBefore(cutoffEpochMillis: Long): Int
}

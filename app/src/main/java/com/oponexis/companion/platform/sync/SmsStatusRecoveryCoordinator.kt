package com.oponexis.companion.platform.sync

import com.oponexis.companion.data.local.CallOutcomeDao
import com.oponexis.companion.domain.repository.SmsActionRepository
import com.oponexis.companion.platform.notifications.SmsActivityNotificationManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsStatusRecoveryCoordinator @Inject constructor(
    private val callOutcomeDao: CallOutcomeDao,
    private val smsActions: SmsActionRepository,
    private val scheduler: MobilePushSyncScheduler,
    private val notifications: SmsActivityNotificationManager,
) {
    suspend fun recover() {
        callOutcomeDao.findSmsReceiptsAwaitingStatus().forEach { call ->
            val receiptId = call.smsReceiptId ?: return@forEach
            val receiptType = call.smsReceiptType ?: return@forEach
            val status = smsActions.deliveryStatus(receiptId, receiptType)
            if (status == null) {
                scheduler.trackSmsReceipt(call.callRef, receiptId, receiptType)
                return@forEach
            }
            callOutcomeDao.updateSmsDeliveryStatus(call.callRef, status.status, status.detail)
            if (status.status in SMS_TERMINAL_STATUSES) {
                notifications.showStatus(
                    status.status,
                    "COMPANION",
                    status.providerMessageId ?: receiptId,
                )
                scheduler.syncActivity()
            } else {
                scheduler.trackSmsReceipt(call.callRef, receiptId, receiptType)
            }
        }
    }
}

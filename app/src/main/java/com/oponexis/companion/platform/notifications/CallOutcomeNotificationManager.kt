package com.oponexis.companion.platform.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.oponexis.companion.MainActivity
import com.oponexis.companion.R
import com.oponexis.companion.domain.model.PendingCallOutcome
import com.oponexis.companion.domain.model.UiLanguage
import com.oponexis.companion.domain.repository.SettingsRepository
import com.oponexis.companion.ui.localization.text
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class CallOutcomeNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    settingsRepository: SettingsRepository,
) {
    @Volatile private var language: UiLanguage = UiLanguage.Polish

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            settingsRepository.preferences.collect { language = it.uiLanguage }
        }
    }

    fun show(pending: PendingCallOutcome) {
        if (!canNotify()) return
        ensureChannel()
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId(pending.callRef),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_CALL_OUTCOME, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(pending.displayName ?: language.text("Połączenie zakończone", "Call finished", "Дзвінок завершено"))
            .setContentText(pending.notificationText(language))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_launcher_monochrome,
                language.text("Wyślij SMS", "Send SMS", "Надіслати SMS"),
                smsSchedulerIntent(pending.callRef),
            )
            .addAction(
                R.drawable.ic_launcher_monochrome,
                language.text("Pomiń", "Skip", "Пропустити"),
                actionIntent(pending.callRef, CallOutcomeActionReceiver.ACTION_SKIP, 2),
            )
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        notifyWhenPermitted(pending.callRef, notification)
    }

    fun cancel(callRef: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(callRef))
    }

    fun showSmsResult(callRef: String, success: Boolean, failureDetail: String? = null) {
        if (!canNotify()) return
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(
                if (success) language.text("SMS wysłany", "SMS sent", "SMS надіслано")
                else language.text("Nie wysłano SMS", "SMS was not sent", "SMS не надіслано"),
            )
            .setContentText(
                if (success) {
                    language.text("Bramka potwierdziła wysłanie wiadomości.", "The gateway confirmed that the message was sent.", "Шлюз підтвердив надсилання повідомлення.")
                } else {
                    failureDetail ?: language.text("Otwórz Oponexis, sprawdź błąd i spróbuj ponownie.", "Open Oponexis for error details and try again.", "Відкрийте Oponexis, перевірте помилку й повторіть спробу.")
                },
            )
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build()
        notifyWhenPermitted(callRef, notification)
    }

    private fun actionIntent(callRef: String, action: String, offset: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            notificationId(callRef) + offset,
            Intent(context, CallOutcomeActionReceiver::class.java)
                .setAction(action)
                .putExtra(CallOutcomeActionReceiver.EXTRA_CALL_REF, callRef),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun smsSchedulerIntent(callRef: String): PendingIntent = PendingIntent.getActivity(
        context,
        notificationId(callRef) + 1,
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_CALL_OUTCOME, true)
            .putExtra(MainActivity.EXTRA_OPEN_SMS_SCHEDULER, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun notifyWhenPermitted(callRef: String, notification: android.app.Notification) {
        if (!canNotify()) return
        try {
            NotificationManagerCompat.from(context).notify(notificationId(callRef), notification)
        } catch (_: SecurityException) {
            // Permission can be revoked between the explicit check and this call.
        }
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            language.text("Wyniki połączeń", "Call outcome reminders", "Результати дзвінків"),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = language.text("Przypomnienia o wyborze wyniku po zakończeniu połączenia", "Reminders to choose a result after a completed call", "Нагадування про вибір результату після завершення дзвінка")
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notificationId(callRef: String): Int = callRef.hashCode() and Int.MAX_VALUE

    private companion object {
        const val CHANNEL_ID = "call_outcomes_visible_caller"
    }
}

private fun PendingCallOutcome.notificationText(language: UiLanguage): String = when {
    displayName != null && phoneNumber != null -> language.text("$phoneNumber — wybierz wynik połączenia.", "$phoneNumber — choose a call outcome.", "$phoneNumber — оберіть результат дзвінка.")
    phoneNumber != null -> language.text("Połączenie z $phoneNumber zakończone. Wybierz wynik.", "Call with $phoneNumber finished. Choose an outcome.", "Дзвінок із $phoneNumber завершено. Оберіть результат.")
    else -> language.text("Wybierz wynik rozmowy w Oponexis.", "Choose a conversation outcome in Oponexis.", "Оберіть результат розмови в Oponexis.")
}

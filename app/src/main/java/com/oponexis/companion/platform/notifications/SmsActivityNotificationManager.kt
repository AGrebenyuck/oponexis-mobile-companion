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
import androidx.core.content.edit
import com.oponexis.companion.MainActivity
import com.oponexis.companion.R
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
class SmsActivityNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    settingsRepository: SettingsRepository,
) {
    @Volatile private var language: UiLanguage = UiLanguage.Polish

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            settingsRepository.preferences.collect { language = it.uiLanguage }
        }
    }

    fun showStatus(status: String, source: String, eventKey: String? = null) {
        if (status !in TERMINAL_STATUSES || !canNotify()) return
        val notificationKey = eventKey?.hashCode()?.toString() ?: source
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (preferences.getString("status-$notificationKey", null) == status && now - preferences.getLong("time-$notificationKey", 0L) < DUPLICATE_WINDOW_MILLIS) return
        preferences.edit { putString("status-$notificationKey", status); putLong("time-$notificationKey", now) }
        ensureChannel()
        val success = status == "SENT" || status == "DELIVERED"
        val statusTitle = when (status) {
            "DELIVERED" -> language.text("SMS dostarczony", "SMS delivered", "SMS доставлено")
            "SENT" -> language.text("SMS wysłany", "SMS sent", "SMS надіслано")
            else -> language.text("Nie wysłano SMS", "SMS was not sent", "SMS не надіслано")
        }
        val title = "$statusTitle · ${sourceLabel(source)}"
        val text = if (success) sourceDescription(source) else language.text(
            "Typ: ${sourceLabel(source)}. Sprawdź szczegóły w Aktywności SMS.",
            "Type: ${sourceLabel(source)}. Check SMS activity for details.",
            "Тип: ${sourceLabel(source)}. Перевірте подробиці в активності SMS.",
        )
        notify(eventKey?.hashCode() ?: source.hashCode(), title, text)
    }

    fun showStalled(providerMessageId: String, phoneNumber: String?) {
        if (!canNotify()) return
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val key = "stalled-${providerMessageId.hashCode()}"
        val now = System.currentTimeMillis()
        if (now - preferences.getLong(key, 0L) < DUPLICATE_WINDOW_MILLIS) return
        preferences.edit { putLong(key, now) }
        ensureChannel()
        notify(providerMessageId.hashCode(), language.text("SMS nadal oczekuje", "SMS is still queued", "SMS все ще очікує"), phoneNumber?.let { language.text("Wiadomość do $it nie została wysłana w ciągu 4 minut.", "The message to $it was not sent within 4 minutes.", "Повідомлення для $it не надіслано протягом 4 хвилин.") } ?: language.text("Wiadomość nie została wysłana w ciągu 4 minut.", "The message was not sent within 4 minutes.", "Повідомлення не надіслано протягом 4 хвилин."))
    }

    private fun sourceDescription(source: String): String = when (source) {
        "BOOKING_FORM" -> language.text("Wysłano formularz rezerwacji.", "Booking form was sent.", "Надіслано формуляр.")
        "FORM_COMPLETED" -> language.text("Wysłano podziękowanie za wypełnienie formularza.", "Thank-you message after the form was sent.", "Надіслано подяку за форму.")
        "APPOINTMENT_CHANGED" -> language.text("Wysłano informację o zmianie terminu.", "Appointment-change message was sent.", "Надіслано повідомлення про зміну часу.")
        "REMINDER" -> language.text("Wysłano przypomnienie o formularzu.", "Form reminder was sent.", "Надіслано нагадування про форму.")
        "CAMPAIGN" -> language.text("Wysłano wiadomość kampanii.", "Campaign message was sent.", "Надіслано кампанію.")
        "COMPANION" -> language.text("Wysłano zwykłą wiadomość.", "Regular message was sent.", "Надіслано звичайне повідомлення.")
        else -> language.text("Wysłano wiadomość systemową.", "System message was sent.", "Надіслано системне повідомлення.")
    }

    private fun sourceLabel(source: String): String = when (source) {
        "BOOKING_FORM" -> language.text("Formularz", "Booking form", "Формуляр")
        "FORM_COMPLETED" -> language.text("Podziękowanie", "Thank-you", "Подяка")
        "APPOINTMENT_CHANGED" -> language.text("Zmiana terminu", "Appointment change", "Зміна часу")
        "REMINDER" -> language.text("Przypomnienie", "Reminder", "Нагадування")
        "CAMPAIGN" -> language.text("Kampania", "Campaign", "Кампанія")
        "COMPANION" -> language.text("Zwykła wiadomość", "Regular message", "Звичайне повідомлення")
        else -> language.text("Wiadomość systemowa", "System message", "Системне повідомлення")
    }

    @SuppressLint("MissingPermission")
    private fun notify(id: Int, title: String, text: String) {
        val intent = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_launcher_monochrome).setContentTitle(title).setContentText(text).setContentIntent(intent).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true).build()
        try { NotificationManagerCompat.from(context).notify(id and Int.MAX_VALUE, notification) } catch (_: SecurityException) { }
    }

    private fun canNotify(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, language.text("Aktywność SMS", "SMS activity", "Активність SMS"), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "sms_activity"
        const val PREFERENCES_NAME = "sms_activity_notifications"
        const val DUPLICATE_WINDOW_MILLIS = 10L * 60 * 1_000
        val TERMINAL_STATUSES = setOf("SENT", "DELIVERED", "FAILED", "CANCELLED")
    }
}

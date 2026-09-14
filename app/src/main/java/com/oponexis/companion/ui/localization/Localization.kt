package com.oponexis.companion.ui.localization

import androidx.compose.runtime.staticCompositionLocalOf
import com.oponexis.companion.domain.model.UiLanguage

val LocalUiLanguage = staticCompositionLocalOf { UiLanguage.Polish }

fun UiLanguage.text(polish: String, english: String, ukrainian: String): String = when (this) {
    UiLanguage.Polish -> polish
    UiLanguage.English -> english
    UiLanguage.Ukrainian -> ukrainian
}

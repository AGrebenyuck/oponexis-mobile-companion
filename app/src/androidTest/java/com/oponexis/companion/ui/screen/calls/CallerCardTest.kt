package com.oponexis.companion.ui.screen.calls

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.oponexis.companion.domain.model.CallerCardState
import com.oponexis.companion.domain.model.CallerIdentity
import com.oponexis.companion.ui.theme.OponexisTheme
import com.oponexis.companion.ui.localization.LocalUiLanguage
import com.oponexis.companion.domain.model.UiLanguage
import androidx.compose.runtime.CompositionLocalProvider
import org.junit.Rule
import org.junit.Test

class CallerCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun matchedCardShowsOnlyApprovedIdentityField() {
        composeRule.setContent {
            OponexisTheme {
                CompositionLocalProvider(LocalUiLanguage provides UiLanguage.English) {
                    CallerCard(
                        state = CallerCardState.Matched(CallerIdentity("customer-1", "Anna")),
                        onClear = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Anna").assertIsDisplayed()
        composeRule.onNodeWithText("New client — the first-visit SMS template will be used.").assertIsDisplayed()
        composeRule.onNodeWithText("customer-1").assertDoesNotExist()
    }

    @Test
    fun unavailableNumberDoesNotShowPreviousIdentity() {
        composeRule.setContent {
            OponexisTheme {
                CompositionLocalProvider(LocalUiLanguage provides UiLanguage.English) {
                    CallerCard(
                        state = CallerCardState.NumberUnavailable,
                        onClear = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Caller number unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Anna").assertDoesNotExist()
    }
}

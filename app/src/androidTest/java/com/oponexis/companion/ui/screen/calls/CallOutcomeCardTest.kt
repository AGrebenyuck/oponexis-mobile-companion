package com.oponexis.companion.ui.screen.calls

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.foundation.lazy.LazyColumn
import com.oponexis.companion.domain.model.CallOutcomeCode
import com.oponexis.companion.domain.model.PendingCallOutcome
import com.oponexis.companion.domain.model.UiLanguage
import com.oponexis.companion.ui.theme.OponexisTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CallOutcomeCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsApprovedChoicesAndReturnsSelection() {
        var selected: CallOutcomeCode? = null
        composeRule.setContent {
            OponexisTheme {
                CallOutcomeCard(
                    pending = pending(),
                    pendingCount = 1,
                    onSelect = { selected = it },
                    onSendSms = {},
                    onDismiss = {},
                    language = UiLanguage.English,
                )
            }
        }

        composeRule.onNodeWithText("Interested").assertIsDisplayed().performClick()
        assertEquals(CallOutcomeCode.Interested, selected)
    }

    @Test
    fun skipKeepsOutcomeOptional() {
        var dismissed = false
        composeRule.setContent {
            OponexisTheme {
                LazyColumn {
                    item {
                        CallOutcomeCard(
                            pending = pending(),
                            pendingCount = 2,
                            onSelect = {},
                            onSendSms = {},
                            onDismiss = { dismissed = true },
                            language = UiLanguage.English,
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("2 calls are waiting for an outcome.").assertIsDisplayed()
        composeRule.onNodeWithText("Skip").performScrollTo().performClick()
        assertTrue(dismissed)
    }

    @Test
    fun skipAllIsAvailableForMultiplePendingCalls() {
        var dismissedAll = false
        composeRule.setContent {
            OponexisTheme {
                LazyColumn {
                    item {
                        CallOutcomeCard(
                            pending = pending(),
                            pendingCount = 3,
                            onSelect = {},
                            onSendSms = {},
                            onDismiss = {},
                            onDismissAll = { dismissedAll = true },
                            language = UiLanguage.English,
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("Skip all").performScrollTo().performClick()
        assertTrue(dismissedAll)
    }

    @Test
    fun sendFormSmsReturnsAction() {
        var sendRequested = false
        composeRule.setContent {
            OponexisTheme {
                CallOutcomeCard(
                    pending = pending(),
                    pendingCount = 1,
                    onSelect = {},
                    onSendSms = { sendRequested = true },
                    onDismiss = {},
                    language = UiLanguage.English,
                )
            }
        }

        composeRule.onNodeWithText("Send SMS").assertIsDisplayed().performClick()
        assertTrue(sendRequested)
    }

    private fun pending() = PendingCallOutcome(
        callRef = "call-ref",
        observedAtEpochMillis = 100,
        disconnectCategory = "remote",
        durationBucket = "short",
        phoneNumber = "+48123456789",
        displayName = null,
    )
}

package com.shohankhan.hisaabos.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.HideAmountsProvider
import com.shohankhan.hisaabos.ui.components.PinPad
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.HisaabTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class UiComponentsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `amount text uses taka sign and bangla digits`() {
        compose.setContent {
            HisaabTheme {
                AmountText(minor = 123_456L)
            }
        }
        compose.onNodeWithText("৳১,২৩৪.৫৬").assertIsDisplayed()
    }

    @Test
    fun `hide amounts replaces the value everywhere`() {
        compose.setContent {
            HisaabTheme {
                HideAmountsProvider(hidden = true, banglaDigits = true) {
                    AmountText(minor = 9_999_900L)
                }
            }
        }
        compose.onNodeWithText("৳ ••••").assertIsDisplayed()
    }

    @Test
    fun `pin pad reports every digit and backspace`() {
        val entered = StringBuilder()
        var backspaces = 0
        compose.setContent {
            HisaabTheme {
                PinPad(
                    onDigit = { entered.append(it) },
                    onBackspace = { backspaces++ },
                )
            }
        }
        listOf("১", "২", "৩", "৪").forEach { compose.onNodeWithText(it).performClick() }
        compose.onNodeWithText("০").performClick()
        assertEquals("12340", entered.toString())
        compose.onNodeWithContentDescription("মুছুন").performClick()
        assertEquals(1, backspaces)
        assertTrue(entered.isNotEmpty())
    }

    @Test
    fun `duplicate warning and save action are reachable`() {
        var saved = false
        val state = com.shohankhan.hisaabos.ui.transactions.AddTxState(
            type = com.shohankhan.hisaabos.domain.model.TxType.EXPENSE,
            amountText = "৫০০",
            accounts = listOf(
                com.shohankhan.hisaabos.data.db.AccountEntity(id = 1, name = "নগদ"),
            ),
            duplicateWarning = true,
        )
        compose.setContent {
            HisaabTheme {
                com.shohankhan.hisaabos.ui.transactions.AddTransactionScreen(
                    state = state,
                    onUpdate = {},
                    onTypeChange = {},
                    onSave = {},
                    onSaveDespiteDuplicate = { saved = true },
                    onDismissDuplicate = {},
                    onCreateAccount = { _, _ -> },
                    onCreateCategory = {},
                    onCreateShop = {},
                    onBack = {},
                )
            }
        }
        compose.onNodeWithText(Bn.duplicateWarning).assertIsDisplayed()
        compose.onNodeWithText(Bn.save).performClick()
        assertTrue(saved)
    }
}

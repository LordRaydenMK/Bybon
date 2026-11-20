package dev.sanastasov.bybon.account.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import org.junit.Rule
import org.junit.Test

class AccountScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun signedOut() {
        paparazzi.bybonSnapshot {
            AccountScreenContent(
                AccountUiState(environmentName = "prd"),
                {},
                {},
            )
        }
    }

    @Test
    fun signedIn() {
        paparazzi.bybonSnapshot {
            AccountScreenContent(
                AccountUiState(environmentName = "prd", email = "john.doe@example.com"),
                {},
                {},
            )
        }
    }

    @Test
    fun confirmSignOut() {
        paparazzi.bybonSnapshot {
            AccountScreenContent(
                AccountUiState(
                    environmentName = "prd",
                    email = "john.doe@example.com",
                    confirmSignOut = true,
                ),
                {},
                {},
            )
        }
    }
}

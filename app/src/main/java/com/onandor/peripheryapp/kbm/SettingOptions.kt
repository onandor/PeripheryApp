package com.onandor.peripheryapp.kbm

import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.kbm.input.KeyMapping
import com.onandor.peripheryapp.kbm.input.TouchpadController
import com.onandor.peripheryapp.utils.IntSettingOption
import com.onandor.peripheryapp.utils.LongSettingOption

class SettingOptions {

    companion object {

        val KEYBOARD_LOCALE_DEFAULT = IntSettingOption(
            value = KeyMapping.Locales.EN_US,
            resourceId = R.string.bt_settings_keyboard_language_en_us
        )

        val keyboardLocales = listOf(
            IntSettingOption(
                value = KeyMapping.Locales.EN_US,
                resourceId = R.string.bt_settings_keyboard_language_en_us
            ),
            IntSettingOption(
                value = KeyMapping.Locales.HU_HU,
                resourceId = R.string.bt_settings_keyboard_language_hu_hu
            )
        )

        val POLLING_RATE_DEFAULT = LongSettingOption(
            value = TouchpadController.PollingRates.AUTO,
            resourceId = R.string.bt_settings_mouse_polling_rate_high
        )

        val pollingRates = listOf(
            LongSettingOption(
                value = TouchpadController.PollingRates.AUTO,
                resourceId = R.string.bt_settings_mouse_polling_rate_automatic
            ),
            LongSettingOption(
                value = TouchpadController.PollingRates.LOW,
                resourceId = R.string.bt_settings_mouse_polling_rate_low
            ),
            LongSettingOption(
                value = TouchpadController.PollingRates.HIGH,
                resourceId = R.string.bt_settings_mouse_polling_rate_high
            )
        )
    }
}
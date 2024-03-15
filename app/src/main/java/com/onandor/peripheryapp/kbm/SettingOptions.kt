package com.onandor.peripheryapp.kbm

import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.kbm.input.KeyMapping
import com.onandor.peripheryapp.kbm.input.TouchpadController
import com.onandor.peripheryapp.utils.BtSettingKeys
import com.onandor.peripheryapp.utils.IntSettingOption
import com.onandor.peripheryapp.utils.LongSettingOption

class SettingOptions {

    companion object {

        val keyboardLocale = listOf(
            IntSettingOption(
                value = KeyMapping.Locales.EN_US,
                resourceId = R.string.bt_settings_keyboard_language_en_us
            ),
            IntSettingOption(
                value = KeyMapping.Locales.HU_HU,
                resourceId = R.string.bt_settings_keyboard_language_hu_hu
            )
        )

        val KEYBOARD_LOCALE_DEFAULT = keyboardLocale[0]

        val pollingRate = listOf(
            LongSettingOption(
                value = TouchpadController.PollingRates.HIGH,
                resourceId = R.string.bt_settings_mouse_polling_rate_high
            ),
            LongSettingOption(
                value = TouchpadController.PollingRates.LOW,
                resourceId = R.string.bt_settings_mouse_polling_rate_low
            )
        )

        val POLLING_RATE_DEFAULT = pollingRate[0]

        val SEND_VOLUME_DEFAULT = BtSettingKeys.SEND_VOLUME_INPUT.defaultValue

        val EXTENDED_KEYBOARD_SHOWN_DEFAULT = BtSettingKeys.EXTENDED_KEYBOARD_SHOWN.defaultValue

        val keyboardReportMode = listOf(
            IntSettingOption(
                value = 0,
                resourceId = R.string.bt_settings_keyboard_report_mode_6_key
            ),
            IntSettingOption(
                value = 1,
                resourceId = R.string.bt_settings_keyboard_report_mode_5_key
            )
        )

        val KEYBOARD_REPORT_MODE_DEFAULT = keyboardReportMode[0]
    }
}
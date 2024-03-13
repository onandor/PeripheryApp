package com.onandor.peripheryapp.kbm.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.kbm.SettingOptions
import com.onandor.peripheryapp.kbm.input.KeyMapping
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.utils.BtSettingKeys
import com.onandor.peripheryapp.utils.IntSettingOption
import com.onandor.peripheryapp.utils.LongSettingOption
import com.onandor.peripheryapp.utils.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BtSettingsUiState(
    val locale: IntSettingOption = SettingOptions.KEYBOARD_LOCALE_DEFAULT,
    val pollingRate: LongSettingOption = SettingOptions.POLLING_RATE_DEFAULT,
    val sendVolume: Boolean = SettingOptions.SEND_VOLUME_DEFAULT
)

@HiltViewModel
class BtSettingsViewModel @Inject constructor(
    private val settings: Settings,
    private val navManager: INavigationManager
): ViewModel() {

    private val localeFlow = settings
        .observe(BtSettingKeys.KEYBOARD_LOCALE, KeyMapping.Locales.EN_US)
        .map { locale ->
            SettingOptions
                .keyboardLocale
                .find { option -> option.value == locale }
                ?: SettingOptions.KEYBOARD_LOCALE_DEFAULT
        }
    private val pollingRateFlow = settings
        .observe(BtSettingKeys.MOUSE_POLLING_RATE, 0)
        .map { pollingRate ->
            SettingOptions
                .pollingRate
                .find { option -> option.value == pollingRate }
                ?: SettingOptions.POLLING_RATE_DEFAULT
        }
    private val sendVolumeFlow = settings
        .observe(BtSettingKeys.SEND_VOLUME_INPUT, false)


    val uiState = combine(
        localeFlow, pollingRateFlow, sendVolumeFlow
    ) { locale, pollingRate, sendVolume ->
        BtSettingsUiState(
            locale = locale,
            pollingRate = pollingRate,
            sendVolume = sendVolume
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            BtSettingsUiState()
        )

    fun onLocaleChanged(locale: Int) {
        viewModelScope.launch {
            settings.save(BtSettingKeys.KEYBOARD_LOCALE, locale)
        }
    }

    fun onPollingRateChanged(pollingRate: Long) {
        viewModelScope.launch {
            settings.save(BtSettingKeys.MOUSE_POLLING_RATE, pollingRate)
        }
    }

    fun onSendVolumeChanged(sendVolume: Boolean) {
        viewModelScope.launch {
            settings.save(BtSettingKeys.SEND_VOLUME_INPUT, sendVolume)
        }
    }

    fun navigateBack() {
        navManager.navigateBack()
    }
}
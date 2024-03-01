package com.onandor.peripheryapp.kbm.viewmodels

import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.navigation.INavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    val navigationManager: INavigationManager
) : ViewModel()
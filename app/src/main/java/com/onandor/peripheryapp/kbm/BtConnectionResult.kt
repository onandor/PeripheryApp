package com.onandor.peripheryapp.kbm

sealed interface BtConnectionResult {

    data object ConnectionEstablished: BtConnectionResult
    data class Error(val message: String): BtConnectionResult
}
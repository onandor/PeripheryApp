package com.onandor.peripheryapp.webcam.stream

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

class Utils {

    companion object {
        fun getIPAddress(): String {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            val addresses = mutableListOf<InetAddress>()
            interfaces.forEach { intf ->
                addresses.addAll(intf.inetAddresses.toList())
            }
            val address = addresses.first {
                !it.isLoopbackAddress && it is Inet4Address
                        && it.hostAddress?.substring(0, 3) == "192" }.hostAddress
            val isIPV4 = address!!.indexOf(':') < 0
            return if (isIPV4) {
                address
            } else {
                val delimiter = address.indexOf('%')
                if (delimiter < 0) {
                    address.toUpperCase(Locale.current)
                } else {
                    address.substring(0, delimiter).toUpperCase(Locale.current)
                }
            }
        }

        fun Int.to2ByteArray() : ByteArray = byteArrayOf(shr(8).toByte(), toByte())
    }
}
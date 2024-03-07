package com.onandor.peripheryapp.kbm.bluetooth

import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings

class Constants {

    companion object {
        const val ID_KEYBOARD = 1
        const val ID_MOUSE = 2
        const val ID_BATTERY = 32

        private val HID_REPORT_DESC = byteArrayOf(
            // Keyboard
            0x05.toByte(), 0x01.toByte(),           // Usage page (Generic Desktop)
            0x09.toByte(), 0x06.toByte(),           // Usage (Keyboard)
            0xA1.toByte(), 0x01.toByte(),           // Collection (Application)
            0x85.toByte(), ID_KEYBOARD.toByte(),    //    Report ID
            0x05.toByte(), 0x07.toByte(),           //       Usage page (Key Codes)           ; Modifier keys
            0x19.toByte(), 0xE0.toByte(),           //       Usage minimum (224)              ; Left control
            0x29.toByte(), 0xE7.toByte(),           //       Usage maximum (231)              ; Right GUI
            0x15.toByte(), 0x00.toByte(),           //       Logical minimum (0)              ; Pressed/not pressed
            0x25.toByte(), 0x01.toByte(),           //       Logical maximum (1)
            0x75.toByte(), 0x01.toByte(),           //       Report size (1)                  ; 1 bit per key
            0x95.toByte(), 0x08.toByte(),           //       Report count (8)                 ; 8 keys total
            0x81.toByte(), 0x02.toByte(),           //       Input (Data, Variable, Absolute) ; Send as data with absolute values
            0x75.toByte(), 0x08.toByte(),           //       Report size (8)
            0x95.toByte(), 0x01.toByte(),           //       Report count (1)
            0x81.toByte(), 0x01.toByte(),           //       Input (Constant)                 ; Reserved byte
            0x75.toByte(), 0x08.toByte(),           //       Report size (8)                  ; 1 byte per key for the key code
            0x95.toByte(), 0x06.toByte(),           //       Report count (6)                 ; 6 keys can be pressed at once
            0x15.toByte(), 0x00.toByte(),           //       Logical Minimum (0)              ; Key codes range between 0-101
            0x25.toByte(), 0x65.toByte(),           //       Logical Maximum (101)
            0x05.toByte(), 0x07.toByte(),           //       Usage page (Key Codes)
            0x19.toByte(), 0x00.toByte(),           //       Usage Minimum (0)                ; Key codes range between 0-101
            0x29.toByte(),0x65.toByte(),            //       Usage Maximum (101)
            0x81.toByte(),0x00.toByte(),            //       Input (Data, Array)              ; Key array (6 keys)
            0xC0.toByte(),                          // End Collection

            // Mouse
            0x05.toByte(), 0x01.toByte(),       // Usage Page (Generic Desktop)
            0x09.toByte(), 0x02.toByte(),       // Usage (Mouse)
            0xA1.toByte(), 0x01.toByte(),       // Collection (Application)
            0x85.toByte(), ID_MOUSE.toByte(),   //    Report ID
            0x09.toByte(), 0x01.toByte(),       //    Usage (Pointer)
            0xA1.toByte(), 0x00.toByte(),       //    Collection (Physical)
            0x05.toByte(), 0x09.toByte(),       //       Usage Page (Buttons)               ; 3 mouse buttons
            0x19.toByte(), 0x01.toByte(),       //       Usage minimum (1)                  ; First button
            0x29.toByte(), 0x03.toByte(),       //       Usage maximum (3)                  ; Last button
            0x15.toByte(), 0x00.toByte(),       //       Logical minimum (0)                ; Buttons can be either pressed or not pressed
            0x25.toByte(), 0x01.toByte(),       //       Logical maximum (1)
            0x75.toByte(), 0x01.toByte(),       //       Report size (1)                    ; 1 bit sent per button
            0x95.toByte(), 0x03.toByte(),       //       Report count (3)                   ; 3 bits total for the 3 buttons
            0x81.toByte(), 0x02.toByte(),       //       Input (Data, Variable, Absolute)   ; Send as data with absolute values
            0x75.toByte(), 0x05.toByte(),       //       Report size (5)                    ; 5 bits of padding to reach a byte
            0x95.toByte(), 0x01.toByte(),       //       Report count (1)                   ; Each bit sent once
            0x81.toByte(), 0x01.toByte(),       //       Input (constant)                   ; Send padding as constant to be ignored
            0x05.toByte(), 0x01.toByte(),       //       Usage page (Generic Desktop)       ; Mouse positions and wheel
            0x09.toByte(), 0x30.toByte(),       //       Usage (X)
            0x09.toByte(), 0x31.toByte(),       //       Usage (Y)
            0x09.toByte(), 0x38.toByte(),       //       Usage (Wheel)
            0x15.toByte(), 0x81.toByte(),       //       Logical minimum (-127)             ; Position values can be between -127 and 127
            0x25.toByte(), 0x7F.toByte(),       //       Logical maximum (127)
            0x75.toByte(), 0x08.toByte(),       //       Report size (8)                    ; Position values can be represented in 8 bits
            0x95.toByte(), 0x03.toByte(),       //       Report count (3)                   ; 3x8 bits total for the 2 coordinates and the wheel
            0x81.toByte(), 0x06.toByte(),       //       Input (Data, Variable, Relative)   ; Send as data with values relative to previous report
            0xC0.toByte(),                      //    End Collection
            0xC0.toByte(),                      // End Collection

            // Battery
            0x05.toByte(), 0x0C.toByte(),                // Usage page (Consumer)
            0x09.toByte(), 0x01.toByte(),                // Usage (Consumer Control)
            0xA1.toByte(), 0x01.toByte(),                // Collection (Application)
            0x85.toByte(), ID_BATTERY.toByte(),          //    Report ID
            0x05.toByte(), 0x01.toByte(),                //    Usage page (Generic Desktop)
            0x09.toByte(), 0x06.toByte(),                //    Usage (Keyboard)
            0xA1.toByte(), 0x02.toByte(),                //    Collection (Logical)
            0x05.toByte(), 0x06.toByte(),                //       Usage page (Generic Device Controls)
            0x09.toByte(), 0x20.toByte(),                //       Usage (Battery Strength)
            0x15.toByte(), 0x00.toByte(),                //       Logical minimum (0)
            0x26.toByte(), 0xff.toByte(), 0x00.toByte(), //       Logical maximum (255)
            0x75.toByte(), 0x08.toByte(),                //       Report size (8)
            0x95.toByte(), 0x01.toByte(),                //       Report count (3)
            0x81.toByte(), 0x02.toByte(),                //       Input (Data, Variable, Absolute)
            0xC0.toByte(),                               //    End Collection
            0xC0.toByte(),                               // End Collection
        )

        private const val SDP_NAME = "PeripheryApp"
        private const val SDP_DESCRIPTION = "Android Bluetooth HID device"
        private const val SDP_PROVIDER = "onandor"
        private const val QOS_TOKEN_RATE = 800
        private const val QOS_TOKEN_BUCKET_SIZE = 9
        private const val QOS_PEAK_BANDWIDTH = 0
        private const val QOS_LATENCY = 11250

        val SDP_RECORD = BluetoothHidDeviceAppSdpSettings(
            SDP_NAME,
            SDP_DESCRIPTION,
            SDP_PROVIDER,
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HID_REPORT_DESC
        )
        val QOS_OUT = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            QOS_TOKEN_RATE,
            QOS_TOKEN_BUCKET_SIZE,
            QOS_PEAK_BANDWIDTH,
            QOS_LATENCY,
            BluetoothHidDeviceAppQosSettings.MAX
        )
    }
}
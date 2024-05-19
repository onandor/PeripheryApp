package com.onandor.peripheryapp.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.nio.charset.Charset
import javax.inject.Inject

class AssetLoader @Inject constructor(@ApplicationContext private val context: Context) {

    fun loadAssetAsString(fileName: String): String {
        return context
            .assets
            .open(fileName)
            .bufferedReader()
            .use(BufferedReader::readText)
    }

    fun loadAssetAsByteArray(fileName: String): ByteArray {
        return context
            .assets
            .open(fileName)
            .readBytes()
    }
}
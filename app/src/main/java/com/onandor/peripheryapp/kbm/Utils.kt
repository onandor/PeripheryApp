package com.onandor.peripheryapp.kbm

import android.content.Context
import android.content.pm.PackageManager

fun isPermissionGranted(context: Context, permission: String): Boolean =
    context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
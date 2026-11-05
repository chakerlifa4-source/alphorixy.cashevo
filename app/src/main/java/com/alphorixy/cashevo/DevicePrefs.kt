package com.alphorixy.cashevo

import android.content.Context
import java.util.UUID

object DevicePrefs {
    private const val PREF_NAME = "device_prefs"
    private const val KEY_DEVICE_UUID = "device_uuid"

    fun getDeviceUUID(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var uuid = prefs.getString(KEY_DEVICE_UUID, null)
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_UUID, uuid).apply()
        }
        return uuid
    }
}
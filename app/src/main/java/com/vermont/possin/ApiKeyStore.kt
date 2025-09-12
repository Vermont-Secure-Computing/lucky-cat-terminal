// ApiKeyStore.kt
package com.vermont.possin

import android.content.Context
import android.util.Log

object ApiKeyStore {
    private const val PREFS = "dogpay_prefs"
    private const val KEY = "api_key"

    fun get(context: Context): String? {
        val v = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        Log.d("ApiKeyStore", "get() -> ${if (v.isNullOrEmpty()) "(empty)" else "******"}")
        return v
    }

    fun set(context: Context, value: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, value ?: "")
            .apply()
        Log.d("ApiKeyStore", "set() <- ${if (value.isNullOrEmpty()) "(empty)" else "******"}")
    }
}

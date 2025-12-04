/*
 * Copyright 2024–2025 Vermont Secure Computing and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
http://www.apache.org/licenses/LICENSE-2.0

 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

/*
 * Copyright 2024–2025 Vermont Secure Computing and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package com.vermont.possin

import android.content.Context
import android.util.Log
import java.util.regex.Pattern

class NanoManager(
    private val context: Context,
    private val addressList: List<String>
) {

    companion object {
        const val PREFS_NAME = "NanoManagerPrefs"
        const val LAST_INDEX_KEY = "lastIndex"

        // Nano address formats:
        // nano_ / xrb_ + 60-char base32 payload
        private val NANO_ADDRESS_REGEX = Pattern.compile(
            "^(nano|xrb)_[13][13456789abcdefghijkmnopqrstuwxyz]{59}$",
            Pattern.CASE_INSENSITIVE
        )

        fun isValidAddress(address: String): Boolean {
            return NANO_ADDRESS_REGEX.matcher(address).matches()
        }
    }

    private val sharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns the next Nano address and its index.
     * Cycles safely if list is smaller than index.
     */
    fun getAddress(): Pair<String, Int> {
        if (addressList.isEmpty()) {
            throw IllegalStateException("Nano address list is empty")
        }

        val lastIndex = getLastIndex()
        val newIndex = if (lastIndex == -1) 0 else lastIndex + 1
        val safeIndex = newIndex % addressList.size

        val address = addressList[safeIndex]
        Log.d("NANO", "Using Nano address index=$safeIndex address=$address")

        return Pair(address, safeIndex)
    }

    fun saveLastIndex(index: Int) {
        with(sharedPreferences.edit()) {
            putInt(LAST_INDEX_KEY, index)
            apply()
        }
    }

    private fun getLastIndex(): Int {
        if (!sharedPreferences.contains(LAST_INDEX_KEY)) {
            return -1
        }
        return sharedPreferences.getInt(LAST_INDEX_KEY, -1)
    }
}

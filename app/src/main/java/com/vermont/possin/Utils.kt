package com.vermont.possin

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.io.IOException

fun loadCryptocurrencies(context: Context): List<CryptoCurrencyInfo> {
    val json: String
    try {
        json = context.assets.open("cryptocurrencies.json").bufferedReader().use { it.readText() }
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        return emptyList()
    }

    val jsonObject = Gson().fromJson(json, JsonObject::class.java)
    val jsonArray = jsonObject.getAsJsonArray("cryptocurrencies")
    val listType = object : TypeToken<List<CryptoCurrencyInfo>>() {}.type
    return Gson().fromJson(jsonArray, listType)
}

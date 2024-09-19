package com.vermont.possin.model

data class Details(
    val apiKey: String,
    val subscriptionLevel: String,
    val active: Boolean,
    val expiresAt: String,
    val hourlyCalls: String,
    val dailyCalls: String,
    val price: Double
)
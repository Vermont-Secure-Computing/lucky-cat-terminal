package com.vermont.possin.network

data class MoneroWalletRequestBody(
    val primaryAddress: String,
    val privateViewKey: String
)
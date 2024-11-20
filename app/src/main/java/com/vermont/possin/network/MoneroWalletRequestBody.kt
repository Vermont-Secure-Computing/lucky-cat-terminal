data class MoneroWalletRequestBody(
    val currentAddress: String? = null,
    val newAddress: String,
    val privateViewKey: String
)
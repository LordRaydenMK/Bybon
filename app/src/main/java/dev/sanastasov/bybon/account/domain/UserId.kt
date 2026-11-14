package dev.sanastasov.bybon.account.domain

@JvmInline
value class UserId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "User id is blank" }
    }
}

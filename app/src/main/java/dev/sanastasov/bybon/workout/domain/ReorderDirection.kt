package dev.sanastasov.bybon.workout.domain

enum class ReorderDirection(
    val offset: Int,
) {
    Up(-1),
    Down(1),
}

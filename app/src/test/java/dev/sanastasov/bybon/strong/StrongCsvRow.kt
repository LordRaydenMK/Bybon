package dev.sanastasov.bybon.strong

data class StrongCsvRow(
    val workoutNumber: Int,
    val date: String,
    val workoutName: String,
    val durationSec: Int,
    val exerciseName: String,
    val setOrder: String,
    val weightKg: Double?,
    val reps: Int?,
    val rpe: Double?,
    val distanceMeters: Double?,
    val seconds: Double?,
    val notes: String?,
    val workoutNotes: String?,
)

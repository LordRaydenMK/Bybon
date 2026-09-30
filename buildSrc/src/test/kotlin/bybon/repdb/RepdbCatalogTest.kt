package bybon.repdb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepdbCatalogTest {

    @Test
    fun `keeps a strength exercise with a hypertrophy or strength goal`() {
        val exercises = parseRepdbCatalog(
            catalog(
                exercise(id = "bench-press", name = "Barbell Bench Press"),
            ),
        )

        assertEquals(listOf("bench-press"), exercises.map { it.id })
        assertEquals("Chest", exercises.single().muscleGroup)
        assertEquals("Barbell", exercises.single().equipment)
    }

    @Test
    fun `drops rows the catalog filter excludes`() {
        val exercises = parseRepdbCatalog(
            catalog(
                exercise(id = "clean", name = "Power Clean", category = "olympic"),
                exercise(id = "bird-dog", name = "Bird Dog", goals = listOf("rehab")),
                exercise(id = "swing", name = "Kettlebell Swing", equipment = "kettlebell"),
                exercise(id = "band-pull", name = "Band Pull Apart", equipment = "loop_band"),
                exercise(id = "boat", name = "Boat Pose"),
                exercise(id = "pilates", name = "Pilates Side Bend"),
                exercise(id = "bench-press", name = "Barbell Bench Press"),
            ),
        )

        assertEquals(listOf("bench-press"), exercises.map { it.id })
    }

    @Test
    fun `maps equipment slugs onto the current load classes`() {
        val exercises = parseRepdbCatalog(
            catalog(
                exercise(
                    id = "squat",
                    name = "Barbell Back Squat",
                    equipment = "barbell",
                    bodyPart = "upper_legs",
                ),
                exercise(id = "trap", name = "Trap Bar Deadlift", equipment = "trap_bar"),
                exercise(id = "curl", name = "Dumbbell Curl", equipment = "dumbbell", bodyPart = "upper_arms"),
                exercise(id = "pull-up", name = "Pull Up", equipment = null, bodyPart = "back"),
                exercise(
                    id = "assisted-pull-ups",
                    name = "Assisted Pull Ups",
                    equipment = "assisted_pullup_machine",
                    bodyPart = "back",
                ),
                exercise(
                    id = "assisted-dips",
                    name = "Machine Assisted Dips",
                    equipment = "dip_machine",
                    bodyPart = "upper_arms",
                ),
                exercise(id = "lat-pulldown", name = "Lat Pulldown", equipment = "cable", bodyPart = "back"),
            ),
        ).associateBy { it.id }

        assertEquals("Barbell", exercises.getValue("squat").equipment)
        assertEquals("Barbell", exercises.getValue("trap").equipment)
        assertEquals("Dumbbell", exercises.getValue("curl").equipment)
        assertEquals("Bodyweight", exercises.getValue("pull-up").equipment)
        assertEquals("AssistedBodyWeight", exercises.getValue("assisted-pull-ups").equipment)
        assertEquals("AssistedBodyWeight", exercises.getValue("assisted-dips").equipment)
        assertEquals("Machine", exercises.getValue("lat-pulldown").equipment)
        assertEquals("Legs", exercises.getValue("squat").muscleGroup)
        assertEquals("Arms", exercises.getValue("curl").muscleGroup)
    }

    @Test
    fun `renders exercise definitions`() {
        val source = renderRepdbCatalog(
            listOf(
                RepdbExercise("bench-press", "Barbell Bench Press", "Chest", "Barbell"),
            ),
        )

        assertTrue(source.contains("\"bench-press\""))
        assertTrue(source.contains("\"Barbell Bench Press\""))
        assertTrue(source.contains("MuscleGroup.Chest"))
        assertTrue(source.contains("Equipment.Barbell"))
    }

    private fun catalog(vararg exercises: String): String = """
        {
          "schema_version": 3,
          "exercises": [
            ${exercises.joinToString(",\n")}
          ]
        }
    """.trimIndent()

    private fun exercise(
        id: String,
        name: String,
        category: String = "strength",
        goals: List<String> = listOf("hypertrophy"),
        equipment: String? = "barbell",
        bodyPart: String = "chest",
    ): String {
        val equipmentJson = equipment?.let { "\"$it\"" } ?: "null"
        val goalsJson = goals.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        return """
            {
              "id": "$id",
              "name_en": "$name",
              "category": "$category",
              "goals": $goalsJson,
              "equipment": $equipmentJson,
              "body_part": "$bodyPart"
            }
        """.trimIndent()
    }
}

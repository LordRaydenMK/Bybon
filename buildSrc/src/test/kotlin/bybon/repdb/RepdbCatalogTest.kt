package bybon.repdb

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
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

        assertTrue(
            source.contains(
                """
                internal val repdbCatalogExercises: List<ExerciseDefinition> = listOf(
                    ExerciseDefinition(
                        "bench-press",
                        "Barbell Bench Press",
                        MuscleGroup.Chest,
                        Equipment.Barbell,
                    ),
                )
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `rejects a catalog that is not schema 3`() {
        val error = assertThrows(RepdbCatalogException::class.java) {
            parseRepdbCatalog(catalog(exercise(id = "bench-press", name = "Barbell Bench Press")).replace("3", "2"))
        }

        assertEquals("RepDB catalog schema is 2, expected 3", error.message)
    }

    @Test
    fun `rejects a row without an id or english name`() {
        val missingId = assertThrows(RepdbCatalogException::class.java) {
            parseRepdbCatalog(catalog(exercise(id = "", name = "Barbell Bench Press")))
        }
        val missingName = assertThrows(RepdbCatalogException::class.java) {
            parseRepdbCatalog(catalog(exercise(id = "bench-press", name = "")))
        }

        assertEquals("RepDB exercise is missing id", missingId.message)
        assertEquals("RepDB exercise bench-press is missing name_en", missingName.message)
    }

    @Test
    fun `rejects a filter result that is not the pinned subset`() {
        val error = assertThrows(RepdbCatalogException::class.java) {
            parseRepdbCatalog(
                catalog(exercise(id = "bench-press", name = "Barbell Bench Press")),
                expectedCount = EXPECTED_REPDB_EXERCISE_COUNT,
            )
        }

        assertEquals("RepDB catalog filter kept 1 exercises, expected 382", error.message)
    }

    @Test
    fun `rejects a body that is not json`() {
        val error = assertThrows(RepdbCatalogException::class.java) {
            parseRepdbCatalog("not json")
        }

        assertEquals("RepDB catalog is not valid JSON", error.message)
    }

    @Test
    fun `reads a cached download without contacting the network`() {
        val cache = tempCache(catalog(exercise(id = "bench-press", name = "Barbell Bench Press")))
        var downloads = 0

        val exercises = loadRepdbCatalog(cache, download = {
            downloads += 1
            RepdbDownload.Failed("offline")
        }, expectedCount = 1)

        assertEquals(listOf("bench-press"), exercises.map { it.id })
        assertEquals(0, downloads)
    }

    @Test
    fun `stores a successful download for the next build`() {
        val cache = tempCache(null)
        val json = catalog(exercise(id = "bench-press", name = "Barbell Bench Press"))
        var downloads = 0

        loadRepdbCatalog(cache, download = {
            downloads += 1
            RepdbDownload.Ok(json)
        }, expectedCount = 1)
        loadRepdbCatalog(cache, download = {
            downloads += 1
            RepdbDownload.Failed("offline")
        }, expectedCount = 1)

        assertEquals(1, downloads)
        assertTrue(cache.isFile)
    }

    @Test
    fun `does not cache a download that fails validation`() {
        val cache = tempCache(null)

        val error = assertThrows(RepdbCatalogException::class.java) {
            loadRepdbCatalog(cache) { RepdbDownload.Ok("not json") }
        }

        assertEquals("RepDB catalog is not valid JSON", error.message)
        assertFalse(cache.exists())
    }

    @Test
    fun `missing cache fails with the download error`() {
        val cache = tempCache(null)
        val http = assertThrows(RepdbCatalogException::class.java) {
            loadRepdbCatalog(cache) { RepdbDownload.HttpStatus(404) }
        }
        val timedOut = assertThrows(RepdbCatalogException::class.java) {
            loadRepdbCatalog(cache) { RepdbDownload.TimedOut }
        }
        val offline = assertThrows(RepdbCatalogException::class.java) {
            loadRepdbCatalog(cache) { RepdbDownload.Failed("connection refused") }
        }

        assertEquals("RepDB download failed: HTTP 404", http.message)
        assertEquals("RepDB download failed: timed out", timedOut.message)
        assertEquals("RepDB download failed: connection refused", offline.message)
    }

    private fun tempCache(contents: String?): File {
        val file = File.createTempFile("repdb-catalog", ".json")
        if (contents == null) {
            file.delete()
        } else {
            file.writeText(contents)
        }
        file.deleteOnExit()
        return file
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

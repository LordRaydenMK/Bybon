package dev.sanastasov.bybon.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("LongMethod")
val MuscleGroupOther: ImageVector
    get() {
        if (cachedMuscleGroupOther != null) return cachedMuscleGroupOther!!

        cachedMuscleGroupOther = ImageVector.Builder(
            name = "muscle_group_other",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(11.1f, 2.1f)
                curveTo(5.55f, 3.15f, 3.23f, 13.05f, 7.12f, 18.9f)
                curveTo(11.4f, 25.43f, 18.9f, 21f, 18.9f, 12f)
                curveTo(18.9f, 5.85f, 15.22f, 1.27f, 11.1f, 2.1f)
                close()
                moveTo(13.42f, 5.33f)
                curveTo(14.17f, 9.22f, 13.95f, 18.3f, 12.97f, 19.95f)
                lineTo(12.9f, 20.1f)
                lineTo(12.9f, 19.72f)
                curveTo(12.9f, 19.57f, 12.97f, 18.52f, 13.05f, 17.4f)
                curveTo(13.2f, 14.7f, 13.2f, 9.22f, 13.05f, 6.45f)
                curveTo(12.97f, 5.33f, 12.9f, 4.27f, 12.9f, 4.12f)
                lineTo(12.9f, 3.75f)
                lineTo(13.05f, 4.12f)
                curveTo(13.12f, 4.27f, 13.28f, 4.88f, 13.42f, 5.33f)
                close()
                moveTo(11.1f, 5.47f)
                curveTo(10.88f, 9.82f, 10.8f, 13.2f, 11.03f, 16.8f)
                curveTo(11.25f, 20.47f, 11.25f, 20.55f, 10.88f, 19.5f)
                curveTo(9.9f, 16.57f, 9.97f, 6.52f, 11.03f, 4.12f)
                curveTo(11.17f, 3.75f, 11.17f, 3.9f, 11.1f, 5.47f)
                close()
                moveTo(8.92f, 5.55f)
                curveTo(8.1f, 9.38f, 8.17f, 15f, 9f, 18.75f)
                curveTo(9.15f, 19.5f, 9.15f, 19.57f, 8.85f, 19.05f)
                curveTo(7.12f, 16.05f, 7.2f, 7.65f, 8.92f, 4.8f)
                curveTo(9.15f, 4.42f, 9.15f, 4.5f, 8.92f, 5.55f)
                close()
                moveTo(15.45f, 5.4f)
                curveTo(16.88f, 8.55f, 16.8f, 16.27f, 15.22f, 19.05f)
                curveTo(14.92f, 19.57f, 14.92f, 19.57f, 15.15f, 18.3f)
                curveTo(15.97f, 14.1f, 15.9f, 8.4f, 14.92f, 4.65f)
                lineTo(14.92f, 4.5f)
                lineTo(15.07f, 4.65f)
                curveTo(15.07f, 4.8f, 15.3f, 5.1f, 15.45f, 5.4f)
                close()
            }
        }.build()

        return cachedMuscleGroupOther!!
    }

private var cachedMuscleGroupOther: ImageVector? = null

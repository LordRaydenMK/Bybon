package dev.sanastasov.bybon.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("LongMethod")
val MuscleGroupArms: ImageVector
    get() {
        if (cachedMuscleGroupArms != null) return cachedMuscleGroupArms!!

        cachedMuscleGroupArms = ImageVector.Builder(
            name = "muscle_group_arms",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(14.92f, 2.02f)
                curveTo(14.85f, 2.02f, 13.72f, 3.38f, 13.57f, 3.6f)
                curveTo(13.42f, 3.9f, 13.5f, 4.12f, 13.8f, 4.5f)
                curveTo(13.95f, 4.65f, 14.1f, 4.88f, 14.1f, 4.95f)
                curveTo(14.1f, 5.17f, 14.32f, 5.33f, 15f, 5.4f)
                lineTo(15.6f, 5.55f)
                lineTo(16.2f, 5.4f)
                curveTo(16.57f, 5.25f, 16.88f, 5.17f, 16.88f, 5.17f)
                curveTo(17.25f, 5.17f, 17.32f, 7.57f, 17.02f, 8.7f)
                curveTo(16.8f, 9.3f, 16.8f, 9.67f, 16.8f, 10.2f)
                curveTo(16.72f, 11.03f, 16.72f, 11.03f, 16.05f, 10.42f)
                curveTo(14.85f, 9.45f, 13.57f, 9.22f, 12.22f, 9.9f)
                lineTo(11.78f, 10.05f)
                lineTo(11.47f, 9.82f)
                curveTo(7.95f, 7.12f, 3.6f, 9.07f, 2.92f, 13.72f)
                curveTo(2.62f, 15.82f, 3.52f, 21.15f, 4.27f, 21.75f)
                curveTo(5.33f, 22.57f, 6.9f, 21.75f, 7.8f, 19.8f)
                curveTo(8.03f, 19.27f, 8.47f, 18.52f, 8.7f, 18.15f)
                curveTo(9.07f, 17.4f, 9.45f, 16.57f, 9.6f, 15.67f)
                curveTo(9.9f, 14.7f, 10.12f, 14.62f, 11.32f, 15.15f)
                curveTo(13.57f, 16.05f, 15.45f, 15.9f, 18.9f, 14.7f)
                curveTo(21.07f, 13.88f, 21.07f, 13.88f, 21.07f, 11.7f)
                curveTo(21.07f, 9.82f, 21f, 9.38f, 19.57f, 5.85f)
                curveTo(19.05f, 4.42f, 18.6f, 3.45f, 18.52f, 3.38f)
                curveTo(18.07f, 2.92f, 15.22f, 1.88f, 14.92f, 2.02f)
                close()
            }
        }.build()

        return cachedMuscleGroupArms!!
    }

private var cachedMuscleGroupArms: ImageVector? = null

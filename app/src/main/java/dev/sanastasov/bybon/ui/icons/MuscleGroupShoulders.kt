package dev.sanastasov.bybon.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("LongMethod")
val MuscleGroupShoulders: ImageVector
    get() {
        if (cachedMuscleGroupShoulders != null) return cachedMuscleGroupShoulders!!

        cachedMuscleGroupShoulders = ImageVector.Builder(
            name = "muscle_group_shoulders",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(10.72f, 4.12f)
                curveTo(10.5f, 4.2f, 10.28f, 4.35f, 10.12f, 4.42f)
                lineTo(9.97f, 4.65f)
                lineTo(9.9f, 5.55f)
                curveTo(9.82f, 7.12f, 9.22f, 7.72f, 7.27f, 8.17f)
                curveTo(3.52f, 8.92f, 1.57f, 11.7f, 2.1f, 15.67f)
                curveTo(2.17f, 16.2f, 2.17f, 16.43f, 2.1f, 16.88f)
                curveTo(1.72f, 18.38f, 2.48f, 19.27f, 4.42f, 19.8f)
                curveTo(5.85f, 20.18f, 6.6f, 19.95f, 7.5f, 18.97f)
                curveTo(8.55f, 17.93f, 8.92f, 17.77f, 12.15f, 17.85f)
                curveTo(15.15f, 17.85f, 15.3f, 17.85f, 16.5f, 19.05f)
                curveTo(17.55f, 20.02f, 18.15f, 20.18f, 19.5f, 19.8f)
                curveTo(21.45f, 19.27f, 22.2f, 18.38f, 21.9f, 16.95f)
                curveTo(21.82f, 16.43f, 21.82f, 16.27f, 21.9f, 15.45f)
                curveTo(22.35f, 11.7f, 20.47f, 8.92f, 16.8f, 8.17f)
                curveTo(14.7f, 7.72f, 14.1f, 7.12f, 14.1f, 5.4f)
                lineTo(14.1f, 4.72f)
                lineTo(13.88f, 4.5f)
                curveTo(13.5f, 4.05f, 11.7f, 3.9f, 10.72f, 4.12f)
                close()
            }
        }.build()

        return cachedMuscleGroupShoulders!!
    }

private var cachedMuscleGroupShoulders: ImageVector? = null

package dev.sanastasov.bybon.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("LongMethod")
val MuscleGroupBack: ImageVector
    get() {
        if (cachedMuscleGroupBack != null) return cachedMuscleGroupBack!!

        cachedMuscleGroupBack = ImageVector.Builder(
            name = "muscle_group_back",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(11.62f, 2.02f)
                curveTo(10.88f, 2.1f, 10.72f, 2.25f, 10.65f, 3.07f)
                curveTo(10.5f, 4.12f, 10.42f, 4.27f, 9f, 4.95f)
                curveTo(8.47f, 5.17f, 7.88f, 5.55f, 7.65f, 5.7f)
                lineTo(7.27f, 6f)
                lineTo(6.3f, 6f)
                lineTo(5.4f, 6.08f)
                lineTo(4.88f, 6.3f)
                curveTo(4.12f, 6.6f, 3.07f, 7.72f, 2.62f, 8.7f)
                curveTo(2.32f, 9.3f, 3.3f, 10.28f, 4.58f, 10.57f)
                curveTo(5.02f, 10.72f, 5.25f, 10.95f, 5.47f, 11.62f)
                curveTo(5.7f, 12.45f, 6.15f, 13.28f, 6.9f, 14.4f)
                curveTo(8.17f, 16.35f, 8.47f, 17.1f, 8.85f, 19.2f)
                curveTo(9.3f, 21.52f, 9.82f, 21.97f, 12f, 21.97f)
                curveTo(14.17f, 21.97f, 14.7f, 21.52f, 15.15f, 19.2f)
                curveTo(15.52f, 17.1f, 15.82f, 16.43f, 17.02f, 14.55f)
                curveTo(17.85f, 13.35f, 18.22f, 12.6f, 18.45f, 11.78f)
                curveTo(18.75f, 10.95f, 18.9f, 10.8f, 19.65f, 10.5f)
                curveTo(21f, 10.05f, 21.68f, 9.3f, 21.3f, 8.62f)
                curveTo(20.47f, 6.97f, 19.27f, 6.08f, 17.85f, 6f)
                curveTo(16.8f, 6f, 16.72f, 6f, 16.35f, 5.7f)
                curveTo(16.2f, 5.55f, 15.6f, 5.25f, 15.07f, 4.95f)
                curveTo(13.65f, 4.27f, 13.35f, 3.9f, 13.35f, 3f)
                curveTo(13.35f, 2.25f, 12.67f, 1.8f, 11.62f, 2.02f)
                close()
            }
        }.build()

        return cachedMuscleGroupBack!!
    }

private var cachedMuscleGroupBack: ImageVector? = null

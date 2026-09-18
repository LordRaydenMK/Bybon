package dev.sanastasov.bybon.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("LongMethod")
val MuscleGroupLegs: ImageVector
    get() {
        if (cachedMuscleGroupLegs != null) return cachedMuscleGroupLegs!!

        cachedMuscleGroupLegs = ImageVector.Builder(
            name = "muscle_group_legs",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(8.62f, 2.48f)
                curveTo(8.55f, 2.7f, 8.4f, 3.15f, 8.32f, 3.52f)
                curveTo(8.32f, 3.9f, 8.17f, 4.42f, 8.03f, 4.65f)
                curveTo(7.35f, 6.45f, 7.35f, 8.1f, 8.17f, 9.67f)
                lineTo(8.47f, 10.35f)
                lineTo(8.47f, 11.25f)
                lineTo(8.47f, 12.07f)
                lineTo(8.32f, 12.53f)
                curveTo(7.8f, 13.57f, 7.8f, 14.55f, 8.32f, 16.12f)
                curveTo(9f, 18.07f, 9.15f, 18.75f, 9f, 19.27f)
                curveTo(9f, 19.5f, 8.92f, 19.72f, 8.85f, 19.88f)
                curveTo(8.85f, 20.18f, 8.4f, 20.7f, 7.72f, 21.22f)
                curveTo(7.27f, 21.6f, 7.42f, 21.82f, 8.55f, 21.97f)
                curveTo(9.15f, 22.05f, 9.38f, 21.97f, 9.6f, 21.75f)
                curveTo(9.75f, 21.68f, 9.9f, 21.6f, 10.05f, 21.52f)
                curveTo(10.5f, 21.45f, 10.65f, 21f, 10.42f, 20.62f)
                curveTo(10.35f, 20.47f, 10.35f, 19.27f, 10.35f, 18.07f)
                curveTo(10.35f, 17.02f, 10.35f, 16.8f, 10.88f, 15.52f)
                curveTo(11.1f, 14.77f, 11.1f, 14.17f, 10.72f, 13.12f)
                curveTo(10.42f, 12.22f, 10.42f, 12.15f, 10.72f, 11.4f)
                curveTo(10.95f, 10.88f, 11.1f, 10.12f, 11.1f, 9.53f)
                curveTo(11.1f, 9.38f, 11.25f, 8.78f, 11.4f, 8.32f)
                curveTo(12f, 6.38f, 12f, 4.88f, 11.32f, 4.2f)
                curveTo(10.28f, 3.15f, 9f, 2.02f, 8.85f, 2.02f)
                curveTo(8.78f, 2.02f, 8.7f, 2.17f, 8.62f, 2.48f)
                close()
                moveTo(14.85f, 2.17f)
                curveTo(14.1f, 2.77f, 12.45f, 4.5f, 12.38f, 4.8f)
                curveTo(12.07f, 5.7f, 12.15f, 6.75f, 12.6f, 8.17f)
                curveTo(12.82f, 8.92f, 12.9f, 9.3f, 12.97f, 9.97f)
                curveTo(13.05f, 10.57f, 13.12f, 10.95f, 13.28f, 11.4f)
                curveTo(13.57f, 12.07f, 13.57f, 12.3f, 13.28f, 13.2f)
                curveTo(12.9f, 14.25f, 12.9f, 14.85f, 13.35f, 15.97f)
                curveTo(13.72f, 17.18f, 13.88f, 18.22f, 13.72f, 18.9f)
                curveTo(13.65f, 19.2f, 13.65f, 19.57f, 13.65f, 19.72f)
                curveTo(13.65f, 19.88f, 13.57f, 20.25f, 13.57f, 20.55f)
                curveTo(13.42f, 21.22f, 13.5f, 21.45f, 13.88f, 21.52f)
                curveTo(14.1f, 21.6f, 14.25f, 21.68f, 14.4f, 21.75f)
                curveTo(14.62f, 21.97f, 14.92f, 22.05f, 15.67f, 21.9f)
                curveTo(16.65f, 21.75f, 16.72f, 21.68f, 16.27f, 21.3f)
                curveTo(14.7f, 19.95f, 14.62f, 19.27f, 15.6f, 16.27f)
                curveTo(16.2f, 14.47f, 16.2f, 13.95f, 15.75f, 12.67f)
                lineTo(15.52f, 12f)
                lineTo(15.52f, 11.17f)
                lineTo(15.52f, 10.35f)
                lineTo(15.82f, 9.75f)
                curveTo(16.65f, 8.17f, 16.72f, 6.52f, 15.97f, 4.65f)
                curveTo(15.82f, 4.42f, 15.67f, 3.82f, 15.67f, 3.45f)
                curveTo(15.38f, 2.02f, 15.3f, 1.88f, 14.85f, 2.17f)
                close()
            }
        }.build()

        return cachedMuscleGroupLegs!!
    }

private var cachedMuscleGroupLegs: ImageVector? = null

package dev.sanastasov.bybon.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("LongMethod")
val MuscleGroupFullBody: ImageVector
    get() {
        if (cachedMuscleGroupFullBody != null) return cachedMuscleGroupFullBody!!

        cachedMuscleGroupFullBody = ImageVector.Builder(
            name = "muscle_group_full_body",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(11.47f, 2.1f)
                curveTo(10.72f, 2.48f, 10.5f, 3.38f, 11.03f, 4.05f)
                curveTo(11.32f, 4.35f, 11.25f, 4.72f, 10.95f, 4.88f)
                curveTo(10.8f, 4.95f, 10.57f, 5.02f, 10.42f, 5.17f)
                curveTo(10.28f, 5.25f, 9.97f, 5.4f, 9.82f, 5.4f)
                curveTo(9.15f, 5.47f, 8.62f, 6.08f, 8.62f, 6.83f)
                curveTo(8.62f, 7.05f, 8.55f, 7.27f, 8.4f, 7.57f)
                curveTo(8.32f, 7.8f, 8.25f, 8.1f, 8.25f, 8.47f)
                curveTo(8.17f, 8.7f, 8.1f, 9.15f, 8.03f, 9.38f)
                lineTo(7.88f, 9.82f)
                lineTo(7.95f, 10.57f)
                curveTo(7.95f, 10.95f, 7.95f, 11.62f, 8.03f, 12.07f)
                curveTo(8.03f, 13.12f, 8.1f, 13.35f, 8.62f, 13.35f)
                curveTo(9.07f, 13.35f, 9.22f, 12.6f, 8.92f, 12.22f)
                curveTo(8.62f, 12f, 8.62f, 11.85f, 9f, 11.03f)
                curveTo(9.22f, 10.42f, 9.22f, 10.2f, 9.22f, 9.82f)
                curveTo(9.22f, 9.45f, 9.3f, 9.3f, 9.45f, 9f)
                curveTo(9.6f, 8.85f, 9.67f, 8.62f, 9.75f, 8.47f)
                lineTo(9.75f, 8.25f)
                lineTo(10.05f, 8.78f)
                lineTo(10.28f, 9.22f)
                lineTo(10.28f, 9.9f)
                curveTo(10.28f, 10.42f, 10.28f, 10.65f, 10.05f, 11.47f)
                curveTo(9.53f, 13.05f, 9.45f, 14.17f, 9.82f, 15.22f)
                curveTo(9.97f, 15.75f, 9.97f, 16.2f, 9.75f, 16.88f)
                curveTo(9.53f, 17.47f, 9.6f, 18.07f, 9.82f, 19.05f)
                curveTo(9.9f, 19.43f, 9.97f, 20.02f, 9.97f, 20.32f)
                lineTo(10.05f, 20.85f)
                lineTo(9.6f, 21.3f)
                curveTo(9.15f, 21.75f, 9.07f, 21.9f, 9.3f, 21.97f)
                curveTo(9.6f, 22.05f, 10.2f, 22.05f, 10.35f, 21.97f)
                curveTo(10.42f, 21.9f, 10.5f, 21.82f, 10.65f, 21.82f)
                curveTo(10.95f, 21.82f, 10.95f, 21.68f, 10.88f, 20.93f)
                curveTo(10.8f, 20.1f, 10.8f, 19.65f, 11.1f, 18.68f)
                curveTo(11.32f, 17.85f, 11.4f, 17.77f, 11.17f, 17.1f)
                lineTo(11.1f, 16.65f)
                lineTo(11.25f, 16.12f)
                curveTo(11.32f, 15.82f, 11.4f, 15.52f, 11.47f, 15.3f)
                curveTo(11.47f, 15.15f, 11.62f, 14.77f, 11.7f, 14.55f)
                curveTo(11.78f, 14.25f, 11.85f, 13.8f, 11.92f, 13.5f)
                curveTo(11.92f, 12.9f, 12.07f, 12.9f, 12.07f, 13.57f)
                curveTo(12.15f, 13.88f, 12.22f, 14.4f, 12.38f, 14.77f)
                curveTo(12.53f, 15.15f, 12.6f, 15.52f, 12.6f, 15.6f)
                curveTo(12.6f, 15.6f, 12.67f, 15.9f, 12.75f, 16.12f)
                lineTo(12.9f, 16.57f)
                lineTo(12.82f, 17.1f)
                curveTo(12.6f, 17.77f, 12.67f, 17.93f, 12.9f, 18.75f)
                curveTo(13.2f, 19.65f, 13.2f, 20.02f, 13.12f, 20.85f)
                curveTo(13.05f, 21.68f, 13.05f, 21.82f, 13.35f, 21.82f)
                curveTo(13.5f, 21.82f, 13.57f, 21.9f, 13.65f, 21.97f)
                curveTo(13.8f, 22.05f, 14.4f, 22.05f, 14.7f, 21.97f)
                curveTo(14.92f, 21.97f, 14.92f, 21.75f, 14.62f, 21.52f)
                curveTo(13.88f, 20.85f, 13.88f, 20.7f, 14.17f, 19.05f)
                curveTo(14.47f, 17.77f, 14.47f, 17.47f, 14.25f, 16.72f)
                curveTo(13.95f, 16.05f, 13.95f, 15.75f, 14.17f, 15.15f)
                curveTo(14.55f, 14.17f, 14.4f, 12.6f, 13.8f, 10.95f)
                curveTo(13.57f, 10.2f, 13.57f, 9.45f, 13.95f, 8.7f)
                lineTo(14.25f, 8.25f)
                lineTo(14.25f, 8.47f)
                curveTo(14.32f, 8.55f, 14.4f, 8.85f, 14.55f, 9f)
                curveTo(14.7f, 9.3f, 14.77f, 9.45f, 14.77f, 9.9f)
                curveTo(14.77f, 10.28f, 14.77f, 10.5f, 15f, 10.88f)
                curveTo(15.3f, 11.7f, 15.38f, 11.92f, 15.15f, 12.15f)
                curveTo(14.62f, 12.67f, 15f, 13.57f, 15.67f, 13.28f)
                curveTo(15.97f, 13.12f, 16.05f, 12.9f, 15.97f, 12.38f)
                curveTo(15.97f, 12.07f, 15.97f, 11.47f, 16.05f, 10.95f)
                lineTo(16.12f, 9.97f)
                lineTo(15.97f, 9.38f)
                curveTo(15.82f, 9f, 15.75f, 8.62f, 15.75f, 8.32f)
                curveTo(15.75f, 8.03f, 15.67f, 7.8f, 15.6f, 7.57f)
                curveTo(15.45f, 7.35f, 15.38f, 7.05f, 15.38f, 6.83f)
                curveTo(15.38f, 6.08f, 14.85f, 5.4f, 14.17f, 5.4f)
                curveTo(14.03f, 5.4f, 13.8f, 5.33f, 13.65f, 5.17f)
                curveTo(13.5f, 5.1f, 13.28f, 4.95f, 13.12f, 4.88f)
                curveTo(12.75f, 4.65f, 12.67f, 4.42f, 12.97f, 3.97f)
                curveTo(13.72f, 2.92f, 12.67f, 1.57f, 11.47f, 2.1f)
                close()
            }
        }.build()

        return cachedMuscleGroupFullBody!!
    }

private var cachedMuscleGroupFullBody: ImageVector? = null

package dev.sanastasov.bybon.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("LongMethod")
val MuscleGroupCore: ImageVector
    get() {
        if (cachedMuscleGroupCore != null) return cachedMuscleGroupCore!!

        cachedMuscleGroupCore = ImageVector.Builder(
            name = "muscle_group_core",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(7.88f, 2.25f)
                curveTo(5.4f, 3.52f, 4.72f, 6f, 6.08f, 8.62f)
                curveTo(7.12f, 10.5f, 7.5f, 13.42f, 7.05f, 15.3f)
                curveTo(6.67f, 16.88f, 6.75f, 17.85f, 7.5f, 18.97f)
                curveTo(7.72f, 19.27f, 8.03f, 19.8f, 8.17f, 20.1f)
                curveTo(8.92f, 21.45f, 10.05f, 21.97f, 12f, 21.97f)
                curveTo(14.17f, 21.97f, 14.92f, 21.52f, 16.12f, 19.57f)
                curveTo(17.32f, 17.7f, 17.4f, 17.1f, 16.95f, 15.07f)
                curveTo(16.5f, 13.35f, 16.95f, 10.5f, 17.93f, 8.7f)
                curveTo(19.27f, 5.92f, 18.6f, 3.45f, 16.12f, 2.25f)
                curveTo(15.38f, 1.88f, 15.3f, 1.88f, 14.92f, 2.48f)
                curveTo(13.72f, 4.35f, 10.35f, 4.42f, 9.15f, 2.55f)
                curveTo(8.85f, 2.1f, 8.62f, 1.95f, 8.55f, 1.95f)
                curveTo(8.47f, 1.95f, 8.17f, 2.1f, 7.88f, 2.25f)
                close()
                moveTo(8.7f, 6.15f)
                curveTo(9f, 7.35f, 9.22f, 8.25f, 9.53f, 8.92f)
                lineTo(9.82f, 9.6f)
                lineTo(9.67f, 9.67f)
                curveTo(9.22f, 9.9f, 8.62f, 8.4f, 8.4f, 6.75f)
                curveTo(8.32f, 5.85f, 8.55f, 5.4f, 8.7f, 6.15f)
                close()
                moveTo(15.52f, 6.52f)
                curveTo(15.52f, 8.03f, 14.85f, 9.9f, 14.4f, 9.67f)
                curveTo(14.17f, 9.67f, 14.25f, 9.53f, 14.4f, 9.07f)
                curveTo(14.7f, 8.4f, 15f, 7.5f, 15.15f, 6.67f)
                curveTo(15.3f, 5.85f, 15.38f, 5.77f, 15.45f, 5.77f)
                curveTo(15.52f, 5.85f, 15.52f, 6.08f, 15.52f, 6.52f)
                close()
                moveTo(12.38f, 10.05f)
                curveTo(12.67f, 10.5f, 14.03f, 10.95f, 15.07f, 10.88f)
                curveTo(15.67f, 10.8f, 15.67f, 10.95f, 15.22f, 11.17f)
                curveTo(14.85f, 11.32f, 14.55f, 11.32f, 13.72f, 11.17f)
                curveTo(12.9f, 10.95f, 12.82f, 10.95f, 12.53f, 11.25f)
                curveTo(12.15f, 11.55f, 12.07f, 12.9f, 12.45f, 13.12f)
                curveTo(12.9f, 13.42f, 14.55f, 13.42f, 15f, 13.05f)
                curveTo(15.15f, 12.97f, 15.3f, 12.97f, 15.3f, 12.97f)
                curveTo(15.3f, 13.2f, 15.07f, 13.5f, 14.77f, 13.65f)
                lineTo(14.55f, 13.8f)
                lineTo(13.57f, 13.8f)
                lineTo(12.67f, 13.72f)
                lineTo(12.45f, 13.88f)
                lineTo(12.3f, 14.1f)
                lineTo(12.22f, 14.77f)
                curveTo(12.22f, 15.67f, 12.22f, 15.75f, 12.97f, 15.82f)
                curveTo(13.72f, 15.97f, 14.4f, 15.82f, 14.85f, 15.52f)
                curveTo(15.22f, 15.3f, 15.22f, 15.3f, 15f, 15.75f)
                curveTo(14.7f, 16.2f, 14.4f, 16.35f, 13.42f, 16.35f)
                curveTo(12.53f, 16.35f, 12.38f, 16.43f, 12.22f, 16.8f)
                curveTo(12.15f, 17.18f, 12.22f, 17.25f, 12.6f, 17.4f)
                curveTo(12.9f, 17.47f, 13.05f, 17.62f, 12.82f, 17.62f)
                curveTo(12.6f, 17.62f, 12.22f, 18.15f, 12.15f, 18.52f)
                curveTo(12f, 19.05f, 12f, 19.05f, 11.85f, 18.45f)
                curveTo(11.7f, 18.15f, 11.62f, 17.93f, 11.4f, 17.77f)
                curveTo(11.1f, 17.55f, 11.1f, 17.47f, 11.47f, 17.32f)
                lineTo(11.78f, 17.25f)
                lineTo(11.7f, 16.95f)
                curveTo(11.7f, 16.43f, 11.62f, 16.43f, 10.57f, 16.35f)
                curveTo(9.45f, 16.27f, 9.07f, 16.12f, 8.92f, 15.45f)
                lineTo(8.85f, 15.3f)
                lineTo(9.15f, 15.52f)
                curveTo(9.6f, 15.82f, 10.28f, 15.97f, 11.03f, 15.82f)
                curveTo(11.7f, 15.75f, 11.78f, 15.67f, 11.78f, 14.77f)
                lineTo(11.78f, 14.1f)
                lineTo(11.55f, 13.88f)
                lineTo(11.32f, 13.72f)
                lineTo(10.42f, 13.8f)
                curveTo(9.45f, 13.8f, 9.15f, 13.72f, 8.85f, 13.35f)
                curveTo(8.62f, 12.97f, 8.62f, 12.82f, 8.85f, 12.97f)
                curveTo(9.15f, 13.28f, 9.6f, 13.35f, 10.5f, 13.35f)
                curveTo(11.7f, 13.28f, 11.78f, 13.2f, 11.78f, 12.22f)
                curveTo(11.78f, 11.1f, 11.55f, 10.95f, 10.35f, 11.17f)
                curveTo(9.22f, 11.4f, 8.85f, 11.32f, 8.55f, 11.03f)
                curveTo(8.47f, 10.88f, 8.55f, 10.88f, 9f, 10.88f)
                curveTo(10.72f, 10.88f, 11.62f, 10.42f, 11.85f, 9.3f)
                curveTo(12f, 8.7f, 12f, 8.7f, 12.15f, 9.3f)
                curveTo(12.22f, 9.53f, 12.3f, 9.9f, 12.38f, 10.05f)
                close()
            }
        }.build()

        return cachedMuscleGroupCore!!
    }

private var cachedMuscleGroupCore: ImageVector? = null

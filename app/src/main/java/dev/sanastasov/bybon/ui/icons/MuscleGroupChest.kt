package dev.sanastasov.bybon.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("LongMethod")
val MuscleGroupChest: ImageVector
    get() {
        if (cachedMuscleGroupChest != null) return cachedMuscleGroupChest!!

        cachedMuscleGroupChest = ImageVector.Builder(
            name = "muscle_group_chest",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(11.32f, 2.17f)
                lineTo(10.65f, 2.4f)
                lineTo(10.57f, 3f)
                curveTo(10.5f, 3.97f, 10.35f, 4.2f, 9.53f, 4.65f)
                curveTo(8.85f, 5.02f, 8.1f, 5.55f, 8.1f, 5.62f)
                curveTo(8.1f, 5.7f, 8.55f, 5.77f, 8.92f, 5.77f)
                curveTo(10.35f, 5.77f, 11.55f, 6.38f, 11.92f, 7.12f)
                curveTo(12f, 7.2f, 12f, 7.2f, 12.07f, 7.12f)
                curveTo(12.45f, 6.3f, 14.03f, 5.7f, 15.9f, 5.7f)
                curveTo(16.2f, 5.7f, 15.07f, 4.88f, 14.47f, 4.65f)
                curveTo(13.72f, 4.27f, 13.42f, 3.67f, 13.42f, 2.77f)
                curveTo(13.42f, 2.55f, 13.42f, 2.4f, 13.35f, 2.32f)
                curveTo(13.28f, 2.32f, 12f, 1.95f, 12f, 1.95f)
                curveTo(12f, 1.95f, 11.62f, 2.02f, 11.32f, 2.17f)
                close()
                moveTo(6.67f, 6f)
                curveTo(5.02f, 6.52f, 4.12f, 7.88f, 4.2f, 9.6f)
                lineTo(4.27f, 10.28f)
                lineTo(4.05f, 10.65f)
                curveTo(3.45f, 11.62f, 3.23f, 12.6f, 3.45f, 13.95f)
                curveTo(3.52f, 14.32f, 3.6f, 14.92f, 3.67f, 15.3f)
                curveTo(3.82f, 16.5f, 4.27f, 17.18f, 6.52f, 19.5f)
                curveTo(7.88f, 20.85f, 8.1f, 21.07f, 8.1f, 20.93f)
                curveTo(8.1f, 20.47f, 8.85f, 20.77f, 10.12f, 21.6f)
                curveTo(10.5f, 21.82f, 10.72f, 21.97f, 10.8f, 21.97f)
                curveTo(10.95f, 21.75f, 9.97f, 20.18f, 9.07f, 19.05f)
                curveTo(8.7f, 18.68f, 8.32f, 18.15f, 8.17f, 17.93f)
                curveTo(7.95f, 17.47f, 7.95f, 17.47f, 7.72f, 18.07f)
                lineTo(7.57f, 18.52f)
                lineTo(7.5f, 18.3f)
                curveTo(7.42f, 18.15f, 7.27f, 17.7f, 7.2f, 17.32f)
                curveTo(6.9f, 16.35f, 6.67f, 15.82f, 6.15f, 15.3f)
                curveTo(5.7f, 14.77f, 5.7f, 14.77f, 6.08f, 14.25f)
                curveTo(6.67f, 13.42f, 6.83f, 12.3f, 6.45f, 11.17f)
                curveTo(6.38f, 10.88f, 6.3f, 10.5f, 6.3f, 10.35f)
                lineTo(6.3f, 10.2f)
                lineTo(6.6f, 10.65f)
                curveTo(6.97f, 11.25f, 7.27f, 11.62f, 7.88f, 11.85f)
                lineTo(8.32f, 12.07f)
                lineTo(9.15f, 12.07f)
                curveTo(11.4f, 12.07f, 12.53f, 9.9f, 11.47f, 7.57f)
                curveTo(10.95f, 6.38f, 8.4f, 5.55f, 6.67f, 6f)
                close()
                moveTo(15.22f, 6f)
                curveTo(13.12f, 6.3f, 12.3f, 7.2f, 12.3f, 9.3f)
                curveTo(12.3f, 10.8f, 12.67f, 11.55f, 13.72f, 11.92f)
                curveTo(15.15f, 12.53f, 16.8f, 11.85f, 17.55f, 10.5f)
                lineTo(17.7f, 10.12f)
                lineTo(17.7f, 10.42f)
                curveTo(17.7f, 10.57f, 17.62f, 10.95f, 17.55f, 11.25f)
                curveTo(17.25f, 12.3f, 17.4f, 13.42f, 18f, 14.25f)
                curveTo(18.38f, 14.85f, 18.38f, 14.7f, 17.93f, 15.22f)
                curveTo(17.4f, 15.75f, 17.02f, 16.5f, 16.88f, 17.32f)
                curveTo(16.57f, 18.45f, 16.43f, 18.68f, 16.27f, 18f)
                curveTo(16.12f, 17.47f, 16.05f, 17.47f, 15.82f, 17.93f)
                curveTo(15.75f, 18.07f, 15.38f, 18.6f, 15f, 18.97f)
                curveTo(14.1f, 20.18f, 13.05f, 21.75f, 13.2f, 21.97f)
                curveTo(13.28f, 21.97f, 13.5f, 21.82f, 13.8f, 21.68f)
                curveTo(15.07f, 20.77f, 15.9f, 20.47f, 15.9f, 20.85f)
                curveTo(15.9f, 21.15f, 15.97f, 21.15f, 16.5f, 20.47f)
                curveTo(16.8f, 20.18f, 17.47f, 19.5f, 17.93f, 19.05f)
                curveTo(19.72f, 17.32f, 20.4f, 16.12f, 20.4f, 14.77f)
                curveTo(20.4f, 14.62f, 20.47f, 14.25f, 20.55f, 13.95f)
                curveTo(20.77f, 12.9f, 20.55f, 11.55f, 20.02f, 10.72f)
                lineTo(19.8f, 10.35f)
                lineTo(19.8f, 9.3f)
                lineTo(19.8f, 8.32f)
                lineTo(19.5f, 7.8f)
                curveTo(18.75f, 6.3f, 17.4f, 5.7f, 15.22f, 6f)
                close()
                moveTo(7.12f, 12.38f)
                curveTo(7.2f, 12.82f, 7.27f, 13.05f, 7.57f, 13.8f)
                curveTo(8.1f, 14.85f, 8.17f, 15.3f, 8.17f, 16.2f)
                lineTo(8.1f, 16.88f)
                lineTo(8.17f, 16.8f)
                curveTo(8.55f, 16.35f, 8.62f, 15.82f, 8.62f, 14.92f)
                curveTo(8.62f, 13.95f, 8.7f, 13.57f, 9.07f, 13.12f)
                lineTo(9.3f, 12.82f)
                lineTo(8.92f, 12.75f)
                curveTo(8.4f, 12.67f, 7.88f, 12.45f, 7.42f, 12.15f)
                lineTo(7.12f, 11.92f)
                lineTo(7.12f, 12.38f)
                close()
                moveTo(16.65f, 12.07f)
                curveTo(16.5f, 12.3f, 15.67f, 12.67f, 15.15f, 12.75f)
                lineTo(14.7f, 12.82f)
                lineTo(14.92f, 13.12f)
                curveTo(15.3f, 13.5f, 15.38f, 13.88f, 15.38f, 14.85f)
                curveTo(15.38f, 15.75f, 15.45f, 15.97f, 15.75f, 16.57f)
                curveTo(15.9f, 17.02f, 15.97f, 16.95f, 15.9f, 16.05f)
                curveTo(15.9f, 15.07f, 15.97f, 14.77f, 16.43f, 13.8f)
                curveTo(16.95f, 12.67f, 17.1f, 11.62f, 16.65f, 12.07f)
                close()
            }
        }.build()

        return cachedMuscleGroupChest!!
    }

private var cachedMuscleGroupChest: ImageVector? = null

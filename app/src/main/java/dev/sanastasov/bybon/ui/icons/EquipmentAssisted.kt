package dev.sanastasov.bybon.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("LongMethod")
val EquipmentAssisted: ImageVector
    get() {
        if (cachedEquipmentAssisted != null) return cachedEquipmentAssisted!!

        cachedEquipmentAssisted = ImageVector.Builder(
            name = "equipment_assisted",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(7.05f, 2.1f)
                lineTo(6.9f, 2.32f)
                lineTo(5.7f, 2.32f)
                curveTo(4.42f, 2.32f, 4.27f, 2.4f, 4.27f, 2.7f)
                curveTo(4.27f, 3.07f, 4.42f, 3.07f, 5.7f, 3.07f)
                lineTo(6.9f, 3.07f)
                lineTo(7.05f, 3.3f)
                curveTo(7.12f, 3.52f, 7.2f, 3.82f, 7.27f, 4.88f)
                curveTo(7.35f, 6.6f, 7.27f, 6.38f, 8.47f, 7.42f)
                curveTo(9f, 7.95f, 9.53f, 8.47f, 9.67f, 8.62f)
                lineTo(9.97f, 8.85f)
                lineTo(10.12f, 10.12f)
                lineTo(10.28f, 11.4f)
                lineTo(10.2f, 12.3f)
                curveTo(9.82f, 14.77f, 10.2f, 15f, 13.72f, 15.07f)
                lineTo(14.1f, 15.07f)
                lineTo(14.1f, 15.22f)
                curveTo(14.1f, 15.3f, 13.95f, 16.12f, 13.8f, 16.95f)
                curveTo(13.35f, 19.27f, 13.5f, 19.72f, 14.32f, 19.72f)
                curveTo(15f, 19.72f, 15f, 19.65f, 15.82f, 16.95f)
                curveTo(16.72f, 13.57f, 16.72f, 13.57f, 14.62f, 13.12f)
                curveTo(14.03f, 12.97f, 13.5f, 12.82f, 13.42f, 12.75f)
                curveTo(13.35f, 12.67f, 14.03f, 8.92f, 14.17f, 8.78f)
                curveTo(14.17f, 8.7f, 14.77f, 8.17f, 15.38f, 7.57f)
                curveTo(16.05f, 6.97f, 16.57f, 6.45f, 16.57f, 6.38f)
                curveTo(16.65f, 6.22f, 16.65f, 5.55f, 16.72f, 4.88f)
                curveTo(16.8f, 3.67f, 16.88f, 3.45f, 16.95f, 3.3f)
                lineTo(17.1f, 3.07f)
                lineTo(18.3f, 3.07f)
                curveTo(19.57f, 3.07f, 19.72f, 3.07f, 19.72f, 2.7f)
                curveTo(19.72f, 2.4f, 19.57f, 2.32f, 18.22f, 2.32f)
                curveTo(17.55f, 2.32f, 17.02f, 2.32f, 17.02f, 2.25f)
                curveTo(17.02f, 1.88f, 15.97f, 1.8f, 15.67f, 2.1f)
                lineTo(15.52f, 2.32f)
                lineTo(12f, 2.32f)
                lineTo(8.47f, 2.32f)
                lineTo(8.32f, 2.1f)
                curveTo(8.17f, 1.95f, 8.1f, 1.95f, 7.72f, 1.95f)
                curveTo(7.27f, 1.95f, 7.2f, 1.95f, 7.05f, 2.1f)
                close()
                moveTo(15.6f, 3.3f)
                lineTo(15.75f, 3.45f)
                lineTo(15.6f, 4.65f)
                lineTo(15.45f, 5.77f)
                lineTo(14.32f, 6.6f)
                lineTo(13.28f, 7.35f)
                lineTo(12.97f, 7.35f)
                curveTo(12.82f, 7.27f, 12.22f, 7.27f, 11.7f, 7.27f)
                lineTo(10.65f, 7.27f)
                lineTo(9.6f, 6.52f)
                lineTo(8.55f, 5.77f)
                lineTo(8.4f, 4.72f)
                curveTo(8.25f, 3.45f, 8.25f, 3.45f, 8.4f, 3.23f)
                lineTo(8.55f, 3.07f)
                lineTo(12f, 3.07f)
                lineTo(15.52f, 3.07f)
                lineTo(15.6f, 3.3f)
                close()
                moveTo(11.32f, 4.05f)
                curveTo(10.72f, 4.35f, 10.28f, 5.17f, 10.5f, 5.77f)
                curveTo(10.95f, 7.42f, 13.28f, 7.35f, 13.57f, 5.7f)
                curveTo(13.72f, 4.5f, 12.45f, 3.52f, 11.32f, 4.05f)
                close()
                moveTo(9.6f, 14.7f)
                curveTo(8.7f, 15.6f, 10.12f, 16.5f, 12.53f, 16.5f)
                lineTo(13.35f, 16.5f)
                lineTo(13.42f, 16.35f)
                curveTo(13.95f, 15.6f, 13.72f, 15.38f, 12.45f, 15.38f)
                curveTo(11.17f, 15.3f, 10.72f, 15.15f, 10.2f, 14.85f)
                curveTo(9.82f, 14.47f, 9.82f, 14.47f, 9.6f, 14.7f)
                close()
                moveTo(10.57f, 17.85f)
                curveTo(9.3f, 18.68f, 9.3f, 18.68f, 9.82f, 18.82f)
                curveTo(10.28f, 18.82f, 10.28f, 18.9f, 9.82f, 19.43f)
                curveTo(9.38f, 20.1f, 8.47f, 20.93f, 7.72f, 21.52f)
                curveTo(7.35f, 21.75f, 7.05f, 21.97f, 7.05f, 22.05f)
                curveTo(7.05f, 22.05f, 7.88f, 22.05f, 8.92f, 22.05f)
                lineTo(10.88f, 22.05f)
                lineTo(10.88f, 21.52f)
                curveTo(10.95f, 21f, 11.4f, 19.57f, 11.55f, 19.57f)
                curveTo(11.62f, 19.57f, 11.78f, 19.65f, 11.92f, 19.8f)
                curveTo(12f, 19.88f, 12.15f, 19.95f, 12.22f, 19.95f)
                curveTo(12.22f, 19.88f, 12f, 18.07f, 11.85f, 17.32f)
                curveTo(11.85f, 17.02f, 11.85f, 17.02f, 10.57f, 17.85f)
                close()
            }
        }.build()

        return cachedEquipmentAssisted!!
    }

private var cachedEquipmentAssisted: ImageVector? = null

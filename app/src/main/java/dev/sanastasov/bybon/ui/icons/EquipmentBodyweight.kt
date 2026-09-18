package dev.sanastasov.bybon.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("LongMethod")
val EquipmentBodyweight: ImageVector
    get() {
        if (cachedEquipmentBodyweight != null) return cachedEquipmentBodyweight!!

        cachedEquipmentBodyweight = ImageVector.Builder(
            name = "equipment_bodyweight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(19.88f, 7.5f)
                curveTo(19.5f, 7.65f, 19.12f, 8.03f, 18.82f, 8.47f)
                curveTo(18.6f, 8.85f, 18.68f, 8.78f, 17.93f, 8.55f)
                curveTo(17.18f, 8.32f, 16.35f, 8.47f, 14.62f, 9f)
                curveTo(12.75f, 9.6f, 12.38f, 9.67f, 11.92f, 9.67f)
                curveTo(11.17f, 9.6f, 10.35f, 9.82f, 9.75f, 10.28f)
                curveTo(9.6f, 10.5f, 9.07f, 10.72f, 8.25f, 11.03f)
                lineTo(7.12f, 11.55f)
                lineTo(6.38f, 11.55f)
                lineTo(5.7f, 11.62f)
                lineTo(4.65f, 12.15f)
                lineTo(3.6f, 12.67f)
                lineTo(3.07f, 12.67f)
                curveTo(2.17f, 12.67f, 1.8f, 13.05f, 2.1f, 13.8f)
                curveTo(2.1f, 13.95f, 2.17f, 14.25f, 2.25f, 14.55f)
                curveTo(2.4f, 15.52f, 2.55f, 15.67f, 3.67f, 15.6f)
                curveTo(4.65f, 15.52f, 4.8f, 15.3f, 4.27f, 14.85f)
                lineTo(4.05f, 14.55f)
                lineTo(4.12f, 14.17f)
                curveTo(4.2f, 13.95f, 4.2f, 13.72f, 4.27f, 13.72f)
                curveTo(4.35f, 13.65f, 6.9f, 13.2f, 7.35f, 13.2f)
                curveTo(8.03f, 13.2f, 10.8f, 12.67f, 11.7f, 12.45f)
                curveTo(12.07f, 12.3f, 14.32f, 11.92f, 14.7f, 11.92f)
                lineTo(14.85f, 11.92f)
                lineTo(14.77f, 12.22f)
                curveTo(14.47f, 12.9f, 14.55f, 13.35f, 15.07f, 15f)
                curveTo(15.15f, 15.52f, 15.3f, 15.97f, 15.3f, 16.05f)
                curveTo(15.3f, 16.72f, 17.25f, 16.88f, 17.47f, 16.27f)
                curveTo(17.62f, 15.82f, 17.25f, 15.45f, 16.65f, 15.45f)
                curveTo(16.12f, 15.45f, 16.12f, 15.52f, 16.05f, 14.1f)
                lineTo(15.97f, 12.97f)
                lineTo(16.35f, 12.38f)
                lineTo(16.72f, 11.7f)
                lineTo(17.02f, 11.62f)
                lineTo(17.32f, 11.55f)
                lineTo(17.32f, 12.3f)
                lineTo(17.32f, 12.97f)
                lineTo(17.7f, 13.95f)
                curveTo(17.85f, 14.55f, 18.07f, 15.15f, 18.07f, 15.3f)
                curveTo(18.22f, 15.82f, 18.38f, 15.9f, 19.12f, 15.97f)
                curveTo(20.02f, 15.97f, 20.18f, 15.9f, 20.18f, 15.52f)
                curveTo(20.18f, 15.15f, 19.95f, 14.92f, 19.35f, 14.85f)
                curveTo(18.82f, 14.85f, 18.82f, 14.85f, 18.82f, 14.03f)
                curveTo(18.75f, 13.65f, 18.68f, 13.12f, 18.68f, 12.9f)
                curveTo(18.6f, 12.67f, 18.68f, 12.38f, 18.75f, 12f)
                curveTo(18.75f, 11.78f, 18.82f, 11.32f, 18.9f, 11.03f)
                curveTo(18.9f, 10.5f, 18.97f, 10.42f, 19.35f, 10.65f)
                curveTo(20.32f, 11.25f, 21.97f, 10.28f, 21.97f, 9f)
                curveTo(21.97f, 7.95f, 20.93f, 7.2f, 19.88f, 7.5f)
                close()
            }
        }.build()

        return cachedEquipmentBodyweight!!
    }

private var cachedEquipmentBodyweight: ImageVector? = null

package dev.sanastasov.bybon.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("LongMethod")
val EquipmentDumbbell: ImageVector
    get() {
        if (cachedEquipmentDumbbell != null) return cachedEquipmentDumbbell!!

        cachedEquipmentDumbbell = ImageVector.Builder(
            name = "equipment_dumbbell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(2.85f, 7.2f)
                curveTo(2.77f, 7.35f, 2.55f, 7.72f, 2.32f, 8.1f)
                lineTo(1.95f, 8.7f)
                lineTo(1.95f, 12f)
                lineTo(1.95f, 15.22f)
                lineTo(2.17f, 15.67f)
                curveTo(2.32f, 15.9f, 2.55f, 16.27f, 2.77f, 16.57f)
                lineTo(3.07f, 17.1f)
                lineTo(4.58f, 17.1f)
                curveTo(6.3f, 17.1f, 6.08f, 17.18f, 6.75f, 16.12f)
                lineTo(7.2f, 15.38f)
                lineTo(7.2f, 12.07f)
                lineTo(7.2f, 8.7f)
                lineTo(6.67f, 7.88f)
                lineTo(6.15f, 6.97f)
                lineTo(4.58f, 6.97f)
                lineTo(3f, 6.97f)
                lineTo(2.85f, 7.2f)
                close()
                moveTo(17.4f, 7.72f)
                curveTo(17.18f, 8.1f, 16.95f, 8.55f, 16.88f, 8.62f)
                curveTo(16.8f, 8.7f, 16.8f, 9.67f, 16.8f, 12.07f)
                lineTo(16.88f, 15.38f)
                lineTo(17.32f, 16.12f)
                curveTo(17.55f, 16.57f, 17.77f, 16.95f, 17.85f, 16.95f)
                curveTo(17.93f, 17.1f, 18.22f, 17.1f, 19.43f, 17.1f)
                lineTo(20.93f, 17.1f)
                lineTo(21.22f, 16.57f)
                curveTo(21.45f, 16.35f, 21.68f, 15.9f, 21.75f, 15.75f)
                lineTo(21.97f, 15.38f)
                lineTo(21.97f, 12f)
                lineTo(22.05f, 8.62f)
                lineTo(21.52f, 7.8f)
                lineTo(21f, 6.97f)
                lineTo(19.43f, 6.97f)
                lineTo(17.85f, 6.97f)
                lineTo(17.4f, 7.72f)
                close()
                moveTo(7.42f, 10.05f)
                curveTo(7.35f, 10.12f, 7.35f, 10.72f, 7.35f, 12.15f)
                lineTo(7.42f, 14.03f)
                lineTo(7.8f, 14.1f)
                curveTo(8.1f, 14.1f, 8.32f, 14.1f, 8.4f, 14.03f)
                curveTo(8.47f, 13.88f, 8.55f, 10.2f, 8.4f, 10.05f)
                curveTo(8.25f, 9.97f, 7.57f, 9.97f, 7.42f, 10.05f)
                close()
                moveTo(15.6f, 10.05f)
                curveTo(15.52f, 10.12f, 15.45f, 13.72f, 15.6f, 13.95f)
                curveTo(15.6f, 14.17f, 16.57f, 14.17f, 16.57f, 13.95f)
                curveTo(16.72f, 13.72f, 16.65f, 10.12f, 16.57f, 10.05f)
                curveTo(16.5f, 10.05f, 16.27f, 9.97f, 16.12f, 9.97f)
                curveTo(15.9f, 9.97f, 15.67f, 10.05f, 15.6f, 10.05f)
                close()
                moveTo(8.78f, 10.95f)
                curveTo(8.62f, 11.03f, 8.7f, 13.2f, 8.85f, 13.2f)
                curveTo(9f, 13.28f, 14.4f, 13.28f, 14.92f, 13.2f)
                lineTo(15.3f, 13.2f)
                lineTo(15.3f, 12.07f)
                lineTo(15.3f, 10.88f)
                lineTo(12f, 10.88f)
                curveTo(10.28f, 10.88f, 8.78f, 10.88f, 8.78f, 10.95f)
                close()
            }
        }.build()

        return cachedEquipmentDumbbell!!
    }

private var cachedEquipmentDumbbell: ImageVector? = null

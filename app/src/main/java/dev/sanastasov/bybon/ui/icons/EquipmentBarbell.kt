package dev.sanastasov.bybon.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("LongMethod")
val EquipmentBarbell: ImageVector
    get() {
        if (cachedEquipmentBarbell != null) return cachedEquipmentBarbell!!

        cachedEquipmentBarbell = ImageVector.Builder(
            name = "equipment_barbell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(4.2f, 9f)
                curveTo(4.12f, 9.07f, 4.05f, 9.67f, 4.05f, 12f)
                lineTo(4.05f, 14.85f)
                lineTo(4.2f, 15f)
                curveTo(4.35f, 15.22f, 5.1f, 15.3f, 5.33f, 15.07f)
                lineTo(5.47f, 14.92f)
                lineTo(5.47f, 14.03f)
                lineTo(5.47f, 13.12f)
                lineTo(5.7f, 13.12f)
                lineTo(6f, 13.05f)
                lineTo(6f, 12.75f)
                lineTo(6.08f, 12.45f)
                lineTo(12f, 12.45f)
                lineTo(18f, 12.45f)
                lineTo(18f, 12.75f)
                lineTo(18.07f, 13.05f)
                lineTo(18.3f, 13.12f)
                lineTo(18.52f, 13.12f)
                lineTo(18.52f, 13.95f)
                curveTo(18.52f, 15.07f, 18.75f, 15.3f, 19.43f, 15.22f)
                curveTo(19.95f, 15.07f, 19.95f, 15.22f, 19.95f, 12f)
                lineTo(19.95f, 9.07f)
                lineTo(19.8f, 9f)
                curveTo(19.65f, 8.85f, 18.97f, 8.78f, 18.75f, 8.92f)
                curveTo(18.6f, 9f, 18.52f, 9.3f, 18.52f, 10.05f)
                lineTo(18.52f, 10.88f)
                lineTo(18.3f, 10.95f)
                curveTo(18.07f, 10.95f, 18f, 11.03f, 18f, 11.25f)
                lineTo(18f, 11.55f)
                lineTo(12f, 11.55f)
                lineTo(6.08f, 11.55f)
                lineTo(6f, 11.32f)
                curveTo(6f, 11.03f, 5.85f, 10.88f, 5.62f, 10.88f)
                lineTo(5.47f, 10.88f)
                lineTo(5.47f, 9.97f)
                curveTo(5.47f, 8.92f, 5.47f, 8.85f, 4.72f, 8.85f)
                curveTo(4.42f, 8.85f, 4.27f, 8.85f, 4.2f, 9f)
                close()
                moveTo(2.7f, 9.22f)
                curveTo(2.55f, 9.3f, 2.55f, 9.53f, 2.55f, 10.12f)
                lineTo(2.55f, 10.88f)
                lineTo(2.4f, 10.88f)
                curveTo(2.02f, 10.88f, 2.02f, 11.03f, 2.02f, 12.07f)
                lineTo(2.1f, 13.05f)
                lineTo(2.32f, 13.12f)
                lineTo(2.55f, 13.2f)
                lineTo(2.55f, 13.95f)
                curveTo(2.55f, 14.7f, 2.62f, 14.77f, 2.7f, 14.92f)
                curveTo(2.92f, 15f, 3.52f, 15f, 3.75f, 14.85f)
                lineTo(3.9f, 14.77f)
                lineTo(3.9f, 12.07f)
                curveTo(3.9f, 9.67f, 3.9f, 9.3f, 3.75f, 9.22f)
                curveTo(3.6f, 9f, 2.85f, 9f, 2.7f, 9.22f)
                close()
                moveTo(20.25f, 9.22f)
                curveTo(20.1f, 9.38f, 20.1f, 9.75f, 20.1f, 12.07f)
                lineTo(20.1f, 14.7f)
                lineTo(20.25f, 14.85f)
                curveTo(20.47f, 15.07f, 21.07f, 15.07f, 21.38f, 14.85f)
                lineTo(21.52f, 14.62f)
                lineTo(21.52f, 13.88f)
                lineTo(21.52f, 13.12f)
                lineTo(21.68f, 13.12f)
                curveTo(21.97f, 13.12f, 22.05f, 12.97f, 22.05f, 12f)
                curveTo(22.05f, 11.03f, 21.97f, 10.88f, 21.68f, 10.88f)
                curveTo(21.52f, 10.88f, 21.52f, 10.8f, 21.52f, 10.05f)
                curveTo(21.45f, 9.15f, 21.45f, 9.07f, 20.77f, 9.07f)
                curveTo(20.4f, 9.07f, 20.32f, 9.07f, 20.25f, 9.22f)
                close()
            }
        }.build()

        return cachedEquipmentBarbell!!
    }

private var cachedEquipmentBarbell: ImageVector? = null
